package com.fxz.console;

import com.fxz.component.fuled.cat.starter.annotation.EnableCatTracing;
import com.fxz.dnscore.common.TracedThreadExecuteHook;
import com.fxz.fuled.service.annotation.EnableFuledBoot;
import com.fxz.starter.annotation.EnableDnsServer;
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

    public static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }

}
