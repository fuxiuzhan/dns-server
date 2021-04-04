package com.fxz.exporter.mysql;

import com.fxz.dnscore.exporter.Exporter;
import com.fxz.dnscore.objects.BaseRecord;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;

import java.util.List;

/**
 * @author fxz
 */
public class MySqlExporter implements Exporter {
    @Override
    public String name() {
        return "mysqlExpoter";
    }

    @Override
    public void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {

    }
}
