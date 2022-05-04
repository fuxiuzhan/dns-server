package com.fxz.queerer.query.impl;

import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.queerer.Query;
import com.fxz.queerer.CacheOperate;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;

import java.util.List;

/**
 * @author fxz
 */
@Priority
@Slf4j
public class CacheQuery implements Query {
    CacheOperate cacheOperate;

    public CacheQuery(CacheOperate cacheOperate) {
        this.cacheOperate = cacheOperate;
    }

    @Override
    public String name() {
        return "cacheQuery";
    }

    @Trace
    @Override
    public List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        ActiveSpan.tag("class", CacheQuery.class.getName());
        ActiveSpan.tag("query.name", question.name());
        ActiveSpan.tag("query.type", question.type() + "");
        return cacheOperate.get(question.name(), question.type());
    }
}
