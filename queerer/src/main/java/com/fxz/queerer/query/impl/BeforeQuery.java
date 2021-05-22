package com.fxz.queerer.query.impl;

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
@Priority(order = -2)
@Slf4j
public class BeforeQuery implements Query {
    private CacheOperate cacheOperate;

    public BeforeQuery(CacheOperate cacheOperate) {
        this.cacheOperate = cacheOperate;
    }

    @Override
    public String name() {
        return "beforeQuery";
    }

    @Override
    public List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        //execute before cache
        log.info("name->{},queryHost->{}", name(), question.name());
        return null;
    }
}
