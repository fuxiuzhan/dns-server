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
        String str = "chuangmi_camera_039c01>192.168.10.19>78:DF:72:36:D6:99\n" +
                "chuangmi_camera_039c01>192.168.10.43>78:DF:72:4E:81:74\n" +
                "centos-master>192.168.10.201>00:0C:29:ED:11:62\n" +
                "MacBookAir>192.168.10.36>62:A9:FD:1F:4C:9B\n" +
                "DESKTOP-M7P1NB6>192.168.10.124>D4:5D:64:A9:F9:A8\n" +
                "a6f7ccf7>192.168.10.51>00:0C:29:16:CD:68\n" +
                "fuled-server16>192.168.10.100>00:0C:29:79:EE:E5\n" +
                "SynologyNAS>192.168.10.240>00:11:32:19:22:88\n" +
                "chuangmi_camera_039a01>192.168.10.79>94:F8:27:2A:E8:C9\n" +
                "DEFAULT>192.168.10.99>60:6D:3C:A6:3F:8A\n" +
                "chuangmi_camera_ipc019>192.168.10.85>5C:E5:0C:54:A0:8E\n" +
                "Redmi-K50-Ultra>192.168.10.80>A6:76:49:F0:A7:63";

        String[] split = str.split("\n");
        for (String s : split) {
            String[] split1 = s.split(">");
            String name = split1[0];
            String ip = split1[1];
            String mac = split1[2].replace(":", "");
            DHCPSniffer.HostInfo hostInfo1 = new DHCPSniffer.HostInfo();
            hostInfo1.setIp(ip);
            hostInfo1.setHostName(name);
            hostInfo1.setMac(mac);
            redisTemplate.opsForValue().set(PREFIX + hostInfo1.getIp(), JSON.toJSONString(hostInfo1));
        }
        redisTemplate.opsForValue().set(PREFIX + hostInfo.getIp(), JSON.toJSONString(hostInfo));
        log.info("export host info to redis->{}", JSON.toJSONString(hostInfo));
    }
}
