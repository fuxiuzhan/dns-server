package com.fxz.dnscore.exporter;

import com.fxz.dnscore.objects.BaseRecord;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;

import java.util.List;

public interface Exporter {
    default String name() {
        return "defaultExporter";
    }

    default void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {
    }
}
