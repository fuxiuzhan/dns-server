package com.fxz.dnscore.processor.impl;

import com.fxz.dnscore.coder.DnsRecordCoder;
import com.fxz.dnscore.objects.AAAARecord;
import com.fxz.dnscore.objects.BaseRecord;
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
public class AAAAProcessor extends AbstractProcessor {

    QueryManger queryManger;

    public AAAAProcessor(QueryManger queryManger) {
        this.queryManger = queryManger;
        setQueryManger(this.queryManger);
    }

    @Override
    public List<DefaultDnsRawRecord> assemble(List<BaseRecord> records) {
        List<DefaultDnsRawRecord> recordList = new ArrayList<>();
        for (BaseRecord record : records) {
            if (record instanceof AAAARecord) {
                AAAARecord aaaaRecord = (AAAARecord) record;
                recordList.add(DnsRecordCoder.assembleA(aaaaRecord.getHost(), aaaaRecord.getTtl(), aaaaRecord.getIpV6()));
            }
        }
        return recordList;
    }

    @Override
    public BaseRecord decode(byte[] rawData, DnsRecord dnsRecord) {
        return DnsRecordCoder.decodeAAAA(dnsRecord);
    }

    @Override
    public DnsRecordType type() {
        return DnsRecordType.AAAA;
    }
}
