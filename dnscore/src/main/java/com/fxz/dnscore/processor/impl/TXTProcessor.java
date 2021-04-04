package com.fxz.dnscore.processor.impl;

import com.fxz.dnscore.coder.DnsRecordCoder;
import com.fxz.dnscore.objects.ARecord;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.TXTRecord;
import com.fxz.dnscore.processor.AbstractProcessor;
import com.fxz.dnscore.queerer.QueryManger;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fxz
 */
public class TXTProcessor extends AbstractProcessor {
    QueryManger queryManger;

    public TXTProcessor(QueryManger queryManger) {
        this.queryManger = queryManger;
        setQueryManger(this.queryManger);
    }

    @Override
    public List<DefaultDnsRawRecord> assemble(List<BaseRecord> records) {
        List<DefaultDnsRawRecord> recordList = new ArrayList<>();
        for (BaseRecord record : records) {
            if (record instanceof TXTRecord) {
                TXTRecord txtRecord = (TXTRecord) record;
                recordList.add(DnsRecordCoder.assembleTXT(txtRecord.getHost(), txtRecord.getTtl(), txtRecord.getTxt()));
            }
        }
        return recordList;
    }

    @Override
    public BaseRecord decode(byte[] rawData, DnsRecord dnsRecord) {
        return DnsRecordCoder.decodeTXT(dnsRecord);
    }

    @Override
    public DnsRecordType type() {
        return DnsRecordType.TXT;
    }
}
