package com.fxz.console;

import com.fxz.fuled.service.annotation.EnableFuledBoot;
import com.fxz.starter.annotation.EnableDnsServer;
import org.springframework.boot.SpringApplication;

/**
 * @author fxz
 */
@EnableDnsServer
@EnableFuledBoot
public class ConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }

}
