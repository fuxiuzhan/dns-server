package com.fxz.dnscore.processor.impl;

import com.fxz.dnscore.coder.DnsRecordCoder;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.SOARecord;
import com.fxz.dnscore.processor.AbstractProcessor;
import com.fxz.dnscore.queerer.QueryManger;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import org.apache.skywalking.apm.toolkit.trace.Trace;

import java.util.ArrayList;
import java.util.List;

public class SOAProcessor extends AbstractProcessor {
    QueryManger queryManger;

    public SOAProcessor(QueryManger queryManger) {
        this.queryManger = queryManger;
        setQueryManger(this.queryManger);
    }


    @Trace
    @Override
    public List<DefaultDnsRawRecord> assemble(List<BaseRecord> records) {
        List<DefaultDnsRawRecord> recordList = new ArrayList<>();
        for (BaseRecord record : records) {
            if (record instanceof SOARecord) {
                SOARecord soaRecord = (SOARecord) record;
                recordList.add(DnsRecordCoder.assembleSOA(soaRecord.getHost(), soaRecord.getTtl(), soaRecord.getServerName(), soaRecord.getAuthority()
                        , soaRecord.getSerialNo(), soaRecord.getRefreshInternal(), soaRecord.getRetreyInternal(), soaRecord.getLimit(), soaRecord.getITTl()));
            }
        }
        return recordList;
    }

    @Override
    public BaseRecord decode(byte[] rawData,DnsRecord dnsRecord) {
        return DnsRecordCoder.decodeA(dnsRecord);
    }

    @Override
    public DnsRecordType type() {
        return DnsRecordType.SOA;
    }
}
