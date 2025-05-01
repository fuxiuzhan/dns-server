package com.fxz.queerer.resolver.impl;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.component.fuled.cat.starter.component.threadpool.CatTraceWrapper;
import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.common.ThreadPoolConfig;
import com.fxz.dnscore.io.DatagramDnsResponse;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.common.ResponseSemaphore;
import com.fxz.dnscore.processor.Processor;
import com.fxz.dnscore.server.impl.DnsClient;
import com.fxz.dnscore.server.impl.resolver.Resolver;
import com.fxz.fuled.common.chain.Filter;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.common.chain.annotation.FilterProperty;
import com.fxz.queerer.CacheOperate;
import io.netty.handler.codec.dns.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author fxz
 */
@Slf4j
@FilterProperty(filterGroup = Constant.GROUP_QUERY, name = ParentResolver.NAME, order = -20)
public class ParentResolver implements Resolver, Filter<DefaultDnsQuestion, List<BaseRecord>> {
    public static final String NAME = "ParentResolver";

    private int resolveTimeOut;
    private final int DNS_SERVER_PORT = 53;
    private List<String> domainServers;
    private Map<DnsRecordType, Processor> processorMap;
    private AtomicLong counter = new AtomicLong(0);
    private List<CacheOperate> cacheOperates;

    @Value("${dns.query.cache.fixed.ttl:0}")
    private int fixedTtl;

    public void setCacheOperates(List<CacheOperate> cacheOperates) {
        this.cacheOperates = cacheOperates;
    }

    public void setResolveTimeOut(int resolveTimeOut) {
        this.resolveTimeOut = resolveTimeOut;
    }

    public void setDomainServers(List<String> domainServers) {
        this.domainServers = domainServers;
    }

    public void setDnsClient(DnsClient dnsClient) {
        this.dnsClient = dnsClient;
    }

    private DnsClient dnsClient;

    public void setProcessorMap(Map<DnsRecordType, Processor> processorMap) {
        this.processorMap = processorMap;
    }

    @Trace
    @CatTracing
    @Override
    public FutureTask<DatagramDnsResponse> resolve(DnsClient dnsClient, DatagramDnsQuery query) {
        this.dnsClient = dnsClient;
        ResponseSemaphore responseSemaphoreStorage = new ResponseSemaphore();
        responseSemaphoreStorage.setCountDownLatch(new CountDownLatch(1));
        Constant.singleMap.put(query.id(), responseSemaphoreStorage);
        dnsClient.getChannel().writeAndFlush(query).addListener(f -> {
            if (!f.isSuccess()) {
                Constant.singleMap.remove(query.id());
                log.error("query completed ,but error");
            }
        });
        FutureTask<DatagramDnsResponse> futureTask = new FutureTask(() -> {
            ResponseSemaphore responseSemaphore = Constant.singleMap.get(query.id());
            if (responseSemaphore != null) {
                responseSemaphore.getCountDownLatch().await(resolveTimeOut, TimeUnit.SECONDS);
                return responseSemaphore.getResponse();
            }
            return null;
        });
        ThreadPoolConfig.getQueryThreadPool().execute(CatTraceWrapper.buildRunnable(futureTask, ThreadPoolConfig.QUERY_THREAD_POOL));
        return futureTask;
    }

    @Trace
    @Override
    public DatagramDnsResponse resolveSync(DnsClient dnsClient, DatagramDnsQuery query) {
        ActiveSpan.tag("class", ParentResolver.class.getName());
        this.dnsClient = dnsClient;
        if (dnsClient == null) {
            return null;
        }
        Future<DatagramDnsResponse> resolve = resolve(dnsClient, query);
        ActiveSpan.tag("query.complete", Boolean.TRUE + "");
        try {
            if (Objects.nonNull(resolve)) {
                DatagramDnsResponse datagramDnsResponse = resolve.get(2, TimeUnit.SECONDS);
                Constant.singleMap.remove(datagramDnsResponse.id());
                return datagramDnsResponse;
            }
        } catch (Exception e) {
            if (Objects.nonNull(resolve)) {
                resolve.cancel(Boolean.TRUE);
            }
            DnsRecord dnsRecord = query.recordAt(DnsSection.QUESTION);
            String name = dnsRecord == null ? "n/a" : dnsRecord.name();
            String type = dnsRecord == null ? "n/a" : dnsRecord.type().name();
            log.error("query parent error,host->{},type->{},error->{}", name, type, e.toString());
            ActiveSpan.tag("query.complete", Boolean.FALSE + "");
            ActiveSpan.error(e);
        }
        return null;
    }

    public List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        if (!Constant.netStat) {
            return null;
        }
        for (String serverAddr : domainServers) {
            ActiveSpan.tag("query.dns.server", serverAddr);
            InetSocketAddress addr = new InetSocketAddress(serverAddr, DNS_SERVER_PORT);
            DatagramDnsQuery query = new DatagramDnsQuery(null, addr, (int) (counter.getAndIncrement() % Integer.MAX_VALUE)).setRecord(
                    DnsSection.QUESTION,
                    new DefaultDnsQuestion(question.name(), question.type()));
            query.setRecursionDesired(true);
            DatagramDnsResponse datagramDnsResponse = resolveSync(dnsClient, query);
            if (datagramDnsResponse != null && datagramDnsResponse.count(DnsSection.ANSWER) > 0) {
                List<BaseRecord> baseRecords = new ArrayList<>();
                int count = datagramDnsResponse.count(DnsSection.ANSWER);
                for (int i = 0; i < count; i++) {
                    DnsRecord dnsRecord = datagramDnsResponse.recordAt(DnsSection.ANSWER, i);
                    Processor processor = processorMap.get(dnsRecord.type());
                    if (processor != null) {
                        baseRecords.add(processor.decode(datagramDnsResponse.getRawData(), dnsRecord));
                    }
                }
                if (cacheOperates != null && cacheOperates.size() > 0) {
                    String host = datagramDnsResponse.recordAt(DnsSection.QUESTION).name();
                    DnsRecordType type = datagramDnsResponse.recordAt(DnsSection.QUESTION).type();
                    int ttl = (int) datagramDnsResponse.recordAt(DnsSection.ANSWER, 0).timeToLive();
                    if (!CollectionUtils.isEmpty(baseRecords)) {
                        for (int i = 0; i < cacheOperates.size(); i++) {
                            cacheOperates.get(i).set(host, type, baseRecords, Math.max(fixedTtl, ttl));
                        }
                    }
                }
                ReferenceCountUtil.release(datagramDnsResponse);
                return baseRecords;
            }
        }
        ActiveSpan.tag("query.dns.result", "null");
        return null;
    }

    @Trace
    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        List<BaseRecord> records = findRecords(question);
        if (CollectionUtils.isEmpty(records)) {
            return invoker.invoke(question);
        }
        return records;
    }
}
