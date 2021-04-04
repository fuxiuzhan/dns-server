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
@Slf4j
@Priority(order = -1)
public class SelfDefineQuery implements Query {
    @Override
    public String name() {
        return "highPriorityQueerer";
    }

    @Override
    public List<BaseRecord> findRecords(DefaultDnsQuestion question){
        log.info("name->{},queryHost->{}", name(), question.name());
        return  null;
    }
}
