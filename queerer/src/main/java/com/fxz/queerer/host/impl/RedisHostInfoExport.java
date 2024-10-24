package com.fxz.queerer.host.impl;

import com.alibaba.fastjson.JSON;
import com.fxz.dnscore.exporter.HostInfoExport;
import com.fxz.dnscore.server.impl.DHCPSniffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 *
 */
@Slf4j
public class RedisHostInfoExport implements HostInfoExport {

    private static final String PREFIX = "host_info_";
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void export(DHCPSniffer.HostInfo hostInfo) {
        redisTemplate.opsForValue().set(PREFIX + hostInfo.getIp(), JSON.toJSONString(hostInfo));
        log.info("export host info to redis->{}", JSON.toJSONString(hostInfo));
    }
}
