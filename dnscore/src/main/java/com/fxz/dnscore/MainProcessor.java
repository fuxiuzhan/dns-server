package com.fxz.dnscore;

import com.dianping.cat.Cat;
import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.exporter.ExporterManager;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.common.ProcessResult;
import com.fxz.dnscore.processor.Processor;
import com.fxz.dnscore.processor.ProcessorManger;
import com.fxz.fuled.dynamic.threadpool.RpcContext;
import com.fxz.fuled.logger.starter.annotation.Monitor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

/**
 * @author fxz
 */
@Slf4j
public class MainProcessor implements InitializingBean {

    private ExporterManager exporterManager;

    private ProcessorManger processorManger;

    public void setExporterManager(ExporterManager exporterManager) {
        this.exporterManager = exporterManager;
    }

    public void setProcessorManger(ProcessorManger processorManger) {
        this.processorManger = processorManger;
    }

    private Map<DnsRecordType, Processor> processorMap;

    public void setProcessorMap(Map<DnsRecordType, Processor> processorMap) {
        this.processorMap = processorMap;
    }

    public boolean filter(ChannelHandlerContext ctx, DatagramDnsQuery query) {
        return Boolean.TRUE;
    }


    @Trace
    public void reject(ChannelHandlerContext ctx, DatagramDnsQuery query) {
        log.warn("rejected closing...");
        ctx.close();
    }

    /**
     * @param query
     * @param response
     */
    @Trace
    public void addAuthorities(DatagramDnsQuery query, DatagramDnsResponse response) {
    }


    /**
     * @param query
     * @param response
     */
    @Trace
    @CatTracing
    public void addAdditions(DatagramDnsQuery query, DatagramDnsResponse response) {
        //response.addRecord(DnsSection.ADDITIONAL, DnsRecordCoder.assembleCNAME(query.recordAt(DnsSection.QUESTION).name(), 10, "c.name.com"));
    }

    /**
     * @param ctx
     * @param query
     * @param response
     */
    @Trace
    @CatTracing
    public void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {
        exporterManager.export(ctx, query, response, records);
    }


    /**
     * https
     *
     * @param question
     * @return
     */
    public Boolean needReplace(DefaultDnsQuestion question) {
        return question.type().intValue() == 65;
    }

    /**
     * @param question
     */
    public void replaceQuestion(DatagramDnsQuery query, DefaultDnsQuestion question, DnsRecordType dnsRecordType) {
        query.setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(question.name(), dnsRecordType));
    }

    /**
     * @param ctx
     * @param query
     */
    @Trace
    @CatTracing
    @Monitor
    public void processDnsQuery(ChannelHandlerContext ctx, DatagramDnsQuery query) {
        if (filter(ctx, query)) {
            DefaultDnsQuestion question = query.recordAt(DnsSection.QUESTION);
            DnsRecordType rawType = question.type();
            Boolean needReplace = needReplace(question);
            if (needReplace || !processorMap.containsKey(question.type())) {
                log.info("processorMap not  containsKey type->{} question ->{}", question.type(), question);
                DefaultDnsQuestion defaultDnsQuestion = new DefaultDnsQuestion(question.name(), DnsRecordType.A);
                query.setRecord(DnsSection.QUESTION, defaultDnsQuestion);
                question = defaultDnsQuestion;
            }
            Processor processor = processorMap.getOrDefault(question.type(), processorMap.get(DnsRecordType.A));
            ProcessResult processResult = processor.process(question, query);
            DatagramDnsResponse response = new DatagramDnsResponse(query.recipient(), query.sender(), query.id());
            response.setRecursionDesired(query.isRecursionDesired());
            response.setAuthoritativeAnswer(query.isRecursionDesired());
            response.setRecursionAvailable(query.isRecursionDesired());
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
            if (needReplace) {
                log.info("replace question section...raw->{}", question.type());
                replaceQuestion(query, question, rawType);
            }
            export(ctx, query, response, processResult.getRecords());
        } else {
            reject(ctx, query);
        }
    }

    @Override
    public void afterPropertiesSet() {
        processorMap = processorManger.getProcessorMap();
    }
}
