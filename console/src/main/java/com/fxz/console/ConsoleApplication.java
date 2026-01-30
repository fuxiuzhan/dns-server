package com.fxz.console;

import com.fxz.component.fuled.cat.starter.annotation.EnableCatTracing;
import com.fxz.console.properties.PersistProperties;
import com.fxz.dnscore.common.TracedThreadExecuteHook;
import com.fxz.fuled.service.annotation.EnableFuledBoot;
import com.fxz.starter.annotation.EnableDnsServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

/**
 * @author fxz
 */
@EnableDnsServer
@EnableFuledBoot
@EnableCatTracing
@Import(TracedThreadExecuteHook.class)
public class ConsoleApplication {

    @Autowired
    private PersistProperties properties;
    public static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }
}
