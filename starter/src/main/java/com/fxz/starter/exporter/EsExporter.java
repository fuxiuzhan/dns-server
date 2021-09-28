package com.fxz.starter.exporter;

import com.alibaba.fastjson.JSON;
import com.fxz.dnscore.annotation.Monitor;
import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.exporter.Exporter;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.exporter.elastic.baserepository.BaseRecordRepository;
import com.fxz.exporter.elastic.baserepository.BaseSourceRepository;
import com.fxz.exporter.elastic.objects.QueryRecord;
import com.fxz.exporter.elastic.objects.SourceRecord;
import com.fxz.queerer.util.CacheUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author fxz
 */
@Slf4j
@Priority(order = 0)
public class EsExporter implements Exporter {
    BaseRecordRepository recordRepository;
    BaseSourceRepository sourceRepository;
    String appName;
    AtomicLong counter = new AtomicLong(0);

    public EsExporter(BaseRecordRepository recordRepository, BaseSourceRepository sourceRepository, String appName) {
        this.recordRepository = recordRepository;
        this.sourceRepository = sourceRepository;
        this.appName = appName;
    }

    @Override
    public String name() {
        return "EsExporter";
    }

    @Override
    @Monitor
    /**
     * TODO
     * consider separate caching and logging
     * two operations have different wight
     * caching process immediately
     * logging scheduled and batching
     */
    public void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {
        log.info("exporter->{},sender->{},type->{},host->{},returns->{}"
                , name()
                , query.sender().getAddress().getHostAddress(), query.recordAt(DnsSection.QUESTION).type().name()
                , query.recordAt(DnsSection.QUESTION).name()
                , records == null ? "n/a" : JSON.toJSONString(records));
        if (records != null && records.size() > 0 && query != null && query.count(DnsSection.QUESTION) > 0) {
            /**
             * record history
             */
            QueryRecord queryRecord = new QueryRecord();
            queryRecord.setAppName(appName);
            queryRecord.setId(System.currentTimeMillis() + "_" + counter.getAndIncrement());
            queryRecord.setAnswerCnt(records.size());
            queryRecord.setHost(query.recordAt(DnsSection.QUESTION).name());
            queryRecord.setIp(query.sender().getAddress().getHostAddress());
            queryRecord.setQueryType(query.recordAt(DnsSection.QUESTION).type().name());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            queryRecord.setDateStr(formatter.format(new Date()));
            queryRecord.setDate(new Date());
            queryRecord.setRes(JSON.toJSONString(records));
            recordRepository.save(queryRecord);
            /**
             * record source
             */
            SourceRecord sourceRecord;
            String id = CacheUtil.assembleKey(query.recordAt(DnsSection.QUESTION).name(), query.recordAt(DnsSection.QUESTION).type());
            Optional<SourceRecord> byId = sourceRepository.findById(id);
            if (byId.isPresent()) {
                sourceRecord = byId.get();
                sourceRecord.setCnt(sourceRecord.getCnt() == null ? 0 : sourceRecord.getCnt() + 1);
            } else {
                sourceRecord = new SourceRecord();
                sourceRecord.setCnt(1);
                sourceRecord.setId(id);
            }
            sourceRecord.setDateStr(formatter.format(new Date()));
            sourceRecord.setQueryType(query.recordAt(DnsSection.QUESTION).type().name());
            sourceRecord.setResult(JSON.toJSONString(records));
            sourceRecord.setLastAccess(new Date());
            sourceRecord.setAnswerCnt(records.size());
            sourceRecord.setAppName(appName);
            sourceRecord.setHost(query.recordAt(DnsSection.QUESTION).name());
            sourceRepository.save(sourceRecord);
        }
    }
}
