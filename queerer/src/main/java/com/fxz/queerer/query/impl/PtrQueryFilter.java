package com.fxz.queerer.query.impl;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.PTRRecord;
import com.fxz.fuled.common.chain.Filter;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.common.chain.annotation.FilterProperty;
import com.fxz.fuled.logger.starter.annotation.Monitor;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsRecordType;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@FilterProperty(filterGroup = Constant.GROUP_QUERY, name = PtrQueryFilter.NAME, order = 10)
public class PtrQueryFilter implements Filter<DefaultDnsQuestion, List<BaseRecord>> {

    @Value("${dns.server.name:}")
    private String nameServer;

    @Value("${dns.query.filter.PtrQueryFilter.enabled:true}")
    private boolean enabled;

    @Value("${dns.query.filter.PtrQueryFilter.allRedirect:false}")
    private boolean allRedirect;
    @Value("#{${dns.server.filter.ptr:{}}}")
    private Map<String, String> ptrMap = new HashMap<>();

    @Value("${dns.server.name.ttl:3600}")
    private int nameServerTtl;
    public static final String NAME = "PtrQueryFilter";

    @Monitor(printParams = false)
    @Trace
    @CatTracing
    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        if (enabled) {
            log.info("name->{},queryHost->{}", NAME, question.name());
            if (question.type().equals(DnsRecordType.PTR) && (allRedirect || ptrMap.containsKey(question.name()))) {
                PTRRecord ptrRecord = new PTRRecord();
                ptrRecord.setPtr(ptrMap.getOrDefault(question.name(), nameServer));
                ptrRecord.setTtl(nameServerTtl);
                ptrRecord.setHost(question.name());
                ptrRecord.setType(DnsRecordType.PTR.name());
                return Arrays.asList(ptrRecord);
            }
            return invoker.invoke(question);
        }
        return invoker.invoke(question);
    }
}
