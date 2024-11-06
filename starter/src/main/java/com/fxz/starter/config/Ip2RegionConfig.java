package com.fxz.starter.config;

import cn.z.ip2region.Ip2Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

@Slf4j
public class Ip2RegionConfig {

    @Value("${fuled.dns.server.export.ip2region.url:https://www.404z.cn/files/ip2region/v3.0.0/data/ip2region.zdb}")
    private String url;

    @PostConstruct
    public void init() {
        try {
            Ip2Region.initByUrl(url);
            log.info("Ip2Region initialized->{}", Ip2Region.initialized());
        } catch (Exception e) {
            log.error("Ip2Region initializing error->{}", e);
        }
    }
}
