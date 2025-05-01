package com.fxz.dnscore.queerer;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.fuled.common.chain.FilterChainManger;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.logger.starter.annotation.Monitor;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author fxz
 */

@Slf4j
public class QueryManger {

    @Value("#{${dns.server.filter.ips:{}}}")
    private Map<String, List<String>> filterIps;

    @Autowired
    private FilterChainManger filterChainManger;

    private Invoker<DefaultDnsQuestion, List<BaseRecord>> queryInvoker;

    @PostConstruct
    public void init() {
        queryInvoker = filterChainManger.getInvoker(Constant.GROUP_QUERY, o -> new ArrayList<>());
    }

    @Monitor(printParams = false)
    @Trace
    @CatTracing
    public List<BaseRecord> findRecords(DefaultDnsQuestion question, DatagramDnsQuery query) {
        List<String> orDefault = filterIps.getOrDefault(query.sender().getAddress().getHostAddress(), new ArrayList<>());
        if (orDefault.contains(query.recordAt(DnsSection.QUESTION).type().name())) {
            return new ArrayList<>();
        }
        return queryInvoker.invoke(question);
    }
}
