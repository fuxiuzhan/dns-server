package com.fxz.dnscore.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xiuzhan.fu
 */
public class IpFilter extends ChannelInboundHandlerAdapter {

    private List<String> blackIps=new ArrayList<>();
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }
}