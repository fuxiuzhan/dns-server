package com.fxz.queerer.query.impl;

import com.fxz.dnscore.annotation.Monitor;
import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.queerer.Query;
import com.fxz.queerer.CacheOperate;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;

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

    @Override
    @Monitor
    public List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        return cacheOperate.get(question.name(), question.type());
    }
}
