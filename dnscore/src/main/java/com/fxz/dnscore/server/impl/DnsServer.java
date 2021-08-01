package com.fxz.dnscore.server.impl;

import com.fxz.dnscore.docec.DatagramDnsResponseEncoder;
import com.fxz.dnscore.server.LifeCycle;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsQueryDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author fxz
 */
@Slf4j
public class DnsServer implements LifeCycle {

    private NioEventLoopGroup group = null;
    private String ip;
    private Integer port;

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    private AtomicBoolean stat = new AtomicBoolean(false);

    public void setHandler(SimpleChannelInboundHandler<DatagramDnsQuery> handler) {
        this.handler = handler;
    }

    private SimpleChannelInboundHandler<DatagramDnsQuery> handler;

    @Override
    public String name() {
        return "dnsServer";
    }

    @Override
    public void start() throws InterruptedException {
        if (stat.compareAndSet(false, true)) {
            log.info("dnsServer at host->{},port->{} starting....", ip, port);
            group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group).channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
                                nioDatagramChannel.pipeline().addLast(new DatagramDnsQueryDecoder());
                                nioDatagramChannel.pipeline().addLast(new DatagramDnsResponseEncoder());
                                nioDatagramChannel.pipeline().addLast(handler);
                            }
                        }).option(ChannelOption.SO_BROADCAST, true);
                ChannelFuture future = null;
                if (StringUtils.hasText(ip)) {
                    future = bootstrap.bind(ip, port).sync();
                } else {
                    future = bootstrap.bind(port).sync();
                }
                future.channel().closeFuture().sync();
                log.info("dnsServer at host->{},port->{} started....", ip, port);
            } finally {
                group.shutdownGracefully();
            }
        } else {
            log.warn("dnsServer name->{} already running", name());
        }
    }

    @Override
    public void stop() {
        if (group != null) {
            try {
                group.shutdownGracefully();
                log.info("dnsServer Stoped");
            } catch (Exception e) {
                log.error("dnsServer stop fail reasion->{}", e);
            }
        }
    }

    @Override
    public void restart() {
        throw new RuntimeException("operating not support");
    }
}
