package com.fxz.dnscore.processor.impl;

import com.fxz.dnscore.coder.DnsRecordCoder;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.MXRecord;
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
public class MXProcessor extends AbstractProcessor {
    QueryManger queryManger;

    public MXProcessor(QueryManger queryManger) {
        this.queryManger = queryManger;
        setQueryManger(this.queryManger);
    }

    @Override
    public List<DefaultDnsRawRecord> assemble(List<BaseRecord> records) {
        List<DefaultDnsRawRecord> recordList = new ArrayList<>();
        for (BaseRecord record : records) {
            if (record instanceof MXRecord) {
                MXRecord mxRecord = (MXRecord) record;
                recordList.add(DnsRecordCoder.assembleMX(mxRecord.getHost(), mxRecord.getTtl(), mxRecord.getExchager(), mxRecord.getPriority()));
            }
        }
        return recordList;
    }

    @Override
    public BaseRecord decode(byte[] rawData, DnsRecord dnsRecord) {
        return DnsRecordCoder.decodeMX(dnsRecord);
    }

    @Override
    public DnsRecordType type() {
        return DnsRecordType.MX;
    }
}
