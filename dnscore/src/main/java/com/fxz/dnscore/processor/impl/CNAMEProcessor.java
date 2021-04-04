package com.fxz.dnscore.processor.impl;

import com.fxz.dnscore.coder.DnsRecordCoder;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.processor.AbstractProcessor;
import com.fxz.dnscore.queerer.QueryManger;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;

import java.util.List;

/**
 * @author xiuzhan.fu
 */
public class CNAMEProcessor extends AbstractProcessor {

    QueryManger queryManger;

    public CNAMEProcessor(QueryManger queryManger) {
        this.queryManger = queryManger;
        setQueryManger(this.queryManger);
    }

    @Override
    public List<DefaultDnsRawRecord> assemble(List<BaseRecord> records) {
        return null;
    }

    @Override
    public BaseRecord decode(byte[] rawData, DnsRecord dnsRecord) {
        return DnsRecordCoder.decodeCNAME(rawData, dnsRecord);
    }

    @Override
    public DnsRecordType type() {
        return DnsRecordType.CNAME;
    }
}
