package com.fxz.queerer.query.impl;

import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.queerer.Query;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author fxz
 */
@Priority(order = 3)
@Slf4j
public class LocalQuery implements Query {
    @Override
    public String name() {
        return "localQuery";
    }

    @Override
    public List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        log.info("name->{},queryHost->{}", name(), question.name());
        return null;
    }
}
