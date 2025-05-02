package com.fxz.dnscore.server.impl;

import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.io.DatagramDnsResponse;
import com.fxz.dnscore.objects.common.ResponseSemaphore;
import com.fxz.dnscore.server.LifeCycle;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DatagramDnsQueryEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author fxz
 */
@Slf4j
public class DnsClient implements LifeCycle {
    @Override
    public String name() {
        return "dnsClient";
    }

    private AtomicBoolean stat = new AtomicBoolean();
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private NioEventLoopGroup group = null;
    private Channel ch;

    @Override
    public void start() throws InterruptedException {
        if (stat.compareAndSet(false, true)) {
            log.info("dnsClient name->{} at local starting....", name());
            EventLoopGroup group = new NioEventLoopGroup();
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new DatagramDnsQueryEncoder())
                                    .addLast(new com.fxz.dnscore.docec.DatagramDnsResponseDecoder())
                                    .addLast(new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                            if (msg instanceof DatagramDnsResponse) {
                                                DatagramDnsResponse res = (DatagramDnsResponse) msg;
                                                log.info("DnsClient response ->{}", res);
                                                ResponseSemaphore responseSemaphore = Constant.singleMap.get(res.id());
                                                if (responseSemaphore != null) {
                                                    responseSemaphore.setResponse(res);
                                                    responseSemaphore.getCountDownLatch().countDown();
                                                }
                                            }
                                        }

                                    });
                        }
                    });
            ch = b.bind(0).sync().channel();
            log.info("dnsClient name->{} at local started", name());
            countDownLatch.await();
        } else {
            log.warn("dnsClient name->{} already running", name());
        }
    }

    @Override
    public void stop() {
        countDownLatch.countDown();
        if (group != null) {
            try {
                group.shutdownGracefully();
                log.info("dnsClient Stoped");
            } catch (Exception e) {
                log.error("dnsClient stop fail reasion->{}", e);
            }
        }
    }

    @Override
    public void restart() {
        throw new RuntimeException("operating not support");
    }

    public Channel getChannel() {
        return ch;
    }
}
