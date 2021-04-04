package com.fxz.starter.exporter;

import com.alibaba.fastjson.JSON;
import com.fxz.dnscore.annotation.Monitor;
import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.exporter.Exporter;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.exporter.elastic.objects.QueryRecord;
import com.fxz.exporter.elastic.objects.SourceRecord;
import com.fxz.queerer.util.CacheUtil;
import com.fxz.starter.repository.RecordRepository;
import com.fxz.starter.repository.SourceRepository;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.DateUtils;

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
    RecordRepository recordRepository;
    SourceRepository sourceRepository;
    AtomicLong counter = new AtomicLong(0);

    public EsExporter(RecordRepository recordRepository, SourceRepository sourceRepository) {
        this.recordRepository = recordRepository;
        this.sourceRepository = sourceRepository;
    }

    @Override
    public String name() {
        return "EsExporter";
    }

    @Override
    @Monitor
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
            queryRecord.setId(System.currentTimeMillis() + "_" + counter.getAndIncrement());
            queryRecord.setAnswerCnt(records.size());
            queryRecord.setHost(query.recordAt(DnsSection.QUESTION).name());
            queryRecord.setIp(query.sender().getAddress().getHostAddress());
            queryRecord.setQueryType(query.recordAt(DnsSection.QUESTION).type().name());
            queryRecord.setDate(DateUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
            queryRecord.setRes(JSON.toJSONString(records));
            queryRecord.setTimeMillis(System.currentTimeMillis());
            recordRepository.save(queryRecord);
            /**
             * record source
             */
            SourceRecord sourceRecord;
            String id = CacheUtil.assembleKey(query.recordAt(DnsSection.QUESTION).name(), query.recordAt(DnsSection.QUESTION).type());
            Optional<SourceRecord> byId = sourceRepository.findById(id);
            if (byId.isPresent()) {
                sourceRecord = byId.get();
                sourceRecord.setCounter(sourceRecord.getCounter() + 1);
            } else {
                sourceRecord = new SourceRecord();
                sourceRecord.setCounter(1);
                sourceRecord.setId(id);
            }
            sourceRecord.setResult(JSON.toJSONString(records));
            sourceRecord.setLastAccess(System.currentTimeMillis());
            sourceRepository.save(sourceRecord);
        }
    }
}
