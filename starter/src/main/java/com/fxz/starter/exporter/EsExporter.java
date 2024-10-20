package com.fxz.starter.exporter;

import com.alibaba.fastjson.JSON;
import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.exporter.Exporter;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.server.impl.DHCPSniffer;
import com.fxz.exporter.elastic.baserepository.BaseRecordRepository;
import com.fxz.exporter.elastic.baserepository.BaseSourceRepository;
import com.fxz.exporter.elastic.objects.QueryRecord;
import com.fxz.exporter.elastic.objects.SourceRecord;
import com.fxz.queerer.util.CacheUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author fxz
 */
@Slf4j
@Priority(order = 0)
public class EsExporter implements Exporter {
    private BaseRecordRepository recordRepository;
    private BaseSourceRepository sourceRepository;
    private String appName;
    private AtomicLong counter = new AtomicLong(0);

    private RestHighLevelClient highLevelClient;

    private StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "host_info_";

    public EsExporter(BaseRecordRepository recordRepository, BaseSourceRepository sourceRepository, String appName, RestHighLevelClient highLevelClient, StringRedisTemplate stringRedisTemplate) {
        this.recordRepository = recordRepository;
        this.sourceRepository = sourceRepository;
        this.appName = appName;
        this.highLevelClient = highLevelClient;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String name() {
        return "EsExporter";
    }

    @Override
    /**
     * TODO
     * consider separate caching and logging
     * two operations have different wight
     * caching process immediately
     * logging scheduled and batching
     */
    @Trace
    public void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {
        ActiveSpan.tag("class", EsExporter.class.getName());
        log.info("exporter->{},sender->{},type->{},host->{},returns->{}"
                , name()
                , query.sender().getAddress().getHostAddress(), query.recordAt(DnsSection.QUESTION).type().name()
                , query.recordAt(DnsSection.QUESTION).name()
                , records == null ? "n/a" : JSON.toJSONString(records));
        if (records != null && records.size() > 0 && query != null && query.count(DnsSection.QUESTION) > 0) {
            /**
             * record history
             */
            QueryRecord queryRecord = new QueryRecord();
            queryRecord.setAppName(appName);
            queryRecord.setId(System.currentTimeMillis() + "_" + counter.getAndIncrement());
            queryRecord.setAnswerCnt(records.size());
            queryRecord.setHost(query.recordAt(DnsSection.QUESTION).name());
            String ip = query.sender().getAddress().getHostAddress();
            queryRecord.setIp(ip);
            DHCPSniffer.HostInfo hostInfo = DHCPSniffer.hostInfoMap.get(ip);
            if (Objects.nonNull(hostInfo)) {
                queryRecord.setMac(hostInfo.getMac());
                queryRecord.setHostName(hostInfo.getHostName());
            } else {
                String s = stringRedisTemplate.opsForValue().get(PREFIX + ip);
                if (!StringUtils.isEmpty(s)) {
                    DHCPSniffer.HostInfo o = JSON.parseObject(s, DHCPSniffer.HostInfo.class);
                    if (Objects.nonNull(o) && !StringUtils.isEmpty(o.getIp())) {
                        queryRecord.setMac(o.getMac());
                        queryRecord.setHostName(o.getHostName());
                        log.info("query host info from redis ->{}", s);
                    }
                }
            }
            queryRecord.setQueryType(query.recordAt(DnsSection.QUESTION).type().name());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            queryRecord.setDateStr(formatter.format(new Date()));
            queryRecord.setDate(new Date());
            queryRecord.setRes(JSON.toJSONString(records));
            IndexRequest queryIndexRequest = new IndexRequest(indexName(queryRecord));
            queryIndexRequest.id(queryRecord.getId());
            queryIndexRequest.source(JSON.toJSONString(queryRecord), XContentType.JSON);
            try {
                highLevelClient.index(queryIndexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("flush es error->{}", e);
            }
//            recordRepository.save(queryRecord);
            ActiveSpan.tag("recordType", "QueryRecord");
            ActiveSpan.tag("appName", appName);
            ActiveSpan.tag("id", queryRecord.getId());
            ActiveSpan.tag("answerCnt", queryRecord.getAnswerCnt() + "");
            ActiveSpan.tag("host", queryRecord.getHost());
            ActiveSpan.tag("ip", queryRecord.getIp());
            ActiveSpan.tag("queryType", queryRecord.getQueryType());
            ActiveSpan.tag("date", queryRecord.getDateStr());
            ActiveSpan.tag("res", queryRecord.getRes());

            /**
             * record source
             */
            SourceRecord sourceRecord;
            String id = CacheUtil.assembleKey(query.recordAt(DnsSection.QUESTION).name(), query.recordAt(DnsSection.QUESTION).type());
            Optional<SourceRecord> byId = sourceRepository.findById(id);
            if (byId.isPresent()) {
                sourceRecord = byId.get();
                sourceRecord.setCnt(sourceRecord.getCnt() == null ? 0 : sourceRecord.getCnt() + 1);
            } else {
                sourceRecord = new SourceRecord();
                sourceRecord.setCnt(1);
                sourceRecord.setId(id);
            }
            sourceRecord.setDateStr(formatter.format(new Date()));
            sourceRecord.setQueryType(query.recordAt(DnsSection.QUESTION).type().name());
            sourceRecord.setResult(JSON.toJSONString(records));
            sourceRecord.setLastAccess(new Date());
            sourceRecord.setAnswerCnt(records.size());
            sourceRecord.setAppName(appName);
            sourceRecord.setHost(query.recordAt(DnsSection.QUESTION).name());
            IndexRequest sourceIndexRequest = new IndexRequest(indexName(sourceRecord));
            sourceIndexRequest.id(sourceRecord.getId());
            sourceIndexRequest.source(JSON.toJSONString(sourceRecord), XContentType.JSON);
            try {
                highLevelClient.index(sourceIndexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("flush es error->{}", e);
            }
//            sourceRepository.save(sourceRecord);
        }
    }

    private String indexName(Serializable record) {
        Document annotation = record.getClass().getAnnotation(Document.class);
        if (Objects.nonNull(annotation) && StringUtils.hasText(annotation.indexName())) {
            return annotation.indexName();
        }
        throw new RuntimeException("index not found...");
    }
}
