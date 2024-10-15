package com.fxz.dnscore.processor.impl;

import com.fxz.dnscore.coder.DnsRecordCoder;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.PTRRecord;
import com.fxz.dnscore.processor.AbstractProcessor;
import com.fxz.dnscore.queerer.QueryManger;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import org.apache.skywalking.apm.toolkit.trace.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fxz
 */
public class PTRProcessor extends AbstractProcessor {
    QueryManger queryManger;

    public PTRProcessor(QueryManger queryManger) {
        this.queryManger = queryManger;
        setQueryManger(this.queryManger);
    }

    @Trace
    @Override
    public List<DefaultDnsRawRecord> assemble(List<BaseRecord> records) {
        List<DefaultDnsRawRecord> recordList = new ArrayList<>();
        for (BaseRecord record : records) {
            if (record instanceof PTRRecord) {
                PTRRecord ptrRecord = (PTRRecord) record;
                recordList.add(DnsRecordCoder.assemblePTR(ptrRecord.getHost(), ptrRecord.getTtl(), ptrRecord.getPtr()));
            }
        }
        return recordList;
    }

    @Override
    public BaseRecord decode(byte[] rawData, DnsRecord dnsRecord) {
        return DnsRecordCoder.decodePTR(dnsRecord);
    }

    @Override
    public DnsRecordType type() {
        return DnsRecordType.PTR;
    }
}
