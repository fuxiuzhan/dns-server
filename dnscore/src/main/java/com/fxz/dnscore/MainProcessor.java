package com.fxz.dnscore;

import com.fxz.dnscore.exporter.ExporterManager;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.common.ProcessResult;
import com.fxz.dnscore.processor.Processor;
import com.fxz.dnscore.processor.ProcessorManger;
import com.fxz.fuled.logger.starter.annotation.Monitor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @author fxz
 */
@Slf4j
public class MainProcessor {

    ExporterManager exporterManager;

    ProcessorManger processorManger;

    public void setExporterManager(ExporterManager exporterManager) {
        this.exporterManager = exporterManager;
    }

    public void setProcessorManger(ProcessorManger processorManger) {
        this.processorManger = processorManger;
    }

    private Map<DnsRecordType, Processor> processorMap;

    private DnsRecordType defaultType = DnsRecordType.SOA;

    public void setProcessorMap(Map<DnsRecordType, Processor> processorMap) {
        this.processorMap = processorMap;
    }

    public boolean filter(ChannelHandlerContext ctx, DatagramDnsQuery query) {
        return Boolean.TRUE;
    }


    public void reject(ChannelHandlerContext ctx, DatagramDnsQuery query) {
        log.warn("rejected closing...");
        ctx.close();
    }

    /**
     * @param query
     * @param response
     */
    public void addAuthorities(DatagramDnsQuery query, DatagramDnsResponse response) {
    }


    /**
     * @param query
     * @param response
     */
    public void addAdditions(DatagramDnsQuery query, DatagramDnsResponse response) {
        //response.addRecord(DnsSection.ADDITIONAL, DnsRecordCoder.assembleCNAME(query.recordAt(DnsSection.QUESTION).name(), 10, "c.name.com"));
    }

    /**
     * @param ctx
     * @param query
     * @param response
     */
    public void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {
        exporterManager.export(ctx, query, response, records);
    }

    /**
     * @param ctx
     * @param query
     */
    @Monitor
    public void processDnsQuery(ChannelHandlerContext ctx, DatagramDnsQuery query) {
        if (filter(ctx, query)) {
            DefaultDnsQuestion question = query.recordAt(DnsSection.QUESTION);
            if (processorMap == null) {
                processorMap = processorManger.getProcessorMap();
            }
            Processor processor = processorMap.get(question.type());
            ProcessResult processResult;
            if (processor != null) {
                processResult = processor.process(question);
            } else {
                processResult = processorMap.get(defaultType).process(question);
            }
            DatagramDnsResponse response = new DatagramDnsResponse(query.recipient(), query.sender(), query.id());
            response.addRecord(DnsSection.QUESTION, question);
            if (processResult.getRawRecords() != null && processResult.getRawRecords().size() > 0) {
                for (int i = 0; i < processResult.getRawRecords().size(); i++) {
                    response.addRecord(DnsSection.ANSWER, processResult.getRawRecords().get(i));
                }
            }
            addAuthorities(query, response);
            addAdditions(query, response);
            response.retain();
            query.retain();
            ctx.writeAndFlush(response);
            export(ctx, query, response, processResult.getRecords());
        } else {
            reject(ctx, query);
        }
    }
}
