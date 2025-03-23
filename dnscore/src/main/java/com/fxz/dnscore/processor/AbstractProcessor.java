package com.fxz.dnscore.processor;

import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.common.ProcessResult;
import com.fxz.dnscore.queerer.QueryManger;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsRecordType;

import java.util.List;
import java.util.Map;

/**
 * @author xiuzhan.fu
 */
public abstract class AbstractProcessor implements Processor {

    QueryManger queryManger;
    Map<DnsRecordType, Processor> processorMap;

    public void setQueryManger(QueryManger queryManger) {
        this.queryManger = queryManger;
    }

    public void setProcessorMap(Map<DnsRecordType, Processor> processorMap) {
        this.processorMap = processorMap;
    }

    @Override
    public ProcessResult process(DefaultDnsQuestion question, DatagramDnsQuery query) {
        ProcessResult processResult = new ProcessResult();
        List<BaseRecord> records = queryManger.findRecords(question, query);
        processResult.setRecords(records);
        processResult.setRawRecords(assemble(records));
        return processResult;
    }

    /**
     * assemble  BaseRecord->DefaultDnsRawRecord
     *
     * @param records
     * @return
     */
    public abstract List<DefaultDnsRawRecord> assemble(List<BaseRecord> records);

}
