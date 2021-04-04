package com.fxz.dnscore.server.impl.handler;

import com.fxz.dnscore.MainProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author fxz
 */

public class ServerHandler extends SimpleChannelInboundHandler<DatagramDnsQuery> {
    MainProcessor mainProcessor;

    public void setMainProcessor(MainProcessor mainProcessor) {
        this.mainProcessor = mainProcessor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramDnsQuery datagramDnsQuery) throws Exception {
        mainProcessor.processDnsQuery(channelHandlerContext, datagramDnsQuery);
    }
}
