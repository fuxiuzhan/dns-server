package com.fxz.dnscore.processor;

import com.fxz.dnscore.common.utils.SortUtil;
import io.netty.handler.codec.dns.DnsRecordType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @author fxz
 */
@Slf4j
public class ProcessorManger {

    private List<Processor> processorList;

    public void setProcessorList(List<Processor> processorList) {
        this.processorList = processorList;
    }

    public Map<DnsRecordType, Processor> getProcessorMap() {
        return SortUtil.getProcessorMap(processorList);
    }
}
