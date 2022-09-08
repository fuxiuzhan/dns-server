package com.fxz.console.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "dns.persist")
public class PersistProperties {

    //<ip,hosts>
    //<192_168_10_201,www.baidu.com>
    private Map<String, String> config;
}
