package com.fxz.exporter;

import com.alibaba.fastjson.JSON;
import com.fxz.dnscore.exporter.Exporter;
import com.fxz.dnscore.objects.BaseRecord;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author fxz
 */
@Slf4j
public class DefaultExporter implements Exporter {
    @Override
    public String name() {
        return "DefaultLogExporter";
    }

    @Override
    public void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {
        log.info("sender->{},type->{},host->{},returns->{}"
                , query.sender().getAddress().getHostAddress(), query.recordAt(DnsSection.QUESTION).type().name()
                , query.recordAt(DnsSection.QUESTION).name()
                , records == null ? "n/a" : JSON.toJSONString(records));
    }
}
