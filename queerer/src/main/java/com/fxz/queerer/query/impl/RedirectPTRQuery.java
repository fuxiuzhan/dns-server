package com.fxz.queerer.query.impl;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.PTRRecord;
import com.fxz.dnscore.queerer.Query;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsRecordType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author fxz
 */
@Slf4j
@Priority(order = -1)
public class
RedirectPTRQuery implements Query {
    private String nameServer;
    private int ttl ;

    public RedirectPTRQuery(String nameServer, int ttl) {
        this.nameServer = nameServer;
        this.ttl = ttl;
    }

    @Override
    public String name() {
        return "redirectPTRQuery";
    }

    @CatTracing
    @Override
    public List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        log.info("name->{},queryHost->{}", name(), question.name());
        if (question.type().equals(DnsRecordType.PTR) && StringUtils.hasText(nameServer)) {
            PTRRecord ptrRecord = new PTRRecord();
            ptrRecord.setPtr(nameServer);
            ptrRecord.setTtl(ttl);
            ptrRecord.setHost(question.name());
            ptrRecord.setType(DnsRecordType.PTR.name());
            return Arrays.asList(ptrRecord);
        }
        return null;
    }
}
