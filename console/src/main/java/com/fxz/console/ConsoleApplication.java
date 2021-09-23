package com.fxz.console;

import com.fxz.dnscore.aspect.MethodMonitorAspect;
import com.fxz.starter.annotation.EnableDnsServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@EnableDnsServer
@EnableCaching
@Import(MethodMonitorAspect.class)
public class ConsoleApplication implements ApplicationRunner {


    @Autowired
    private ConfigurableEnvironment environment;
//    @Value("${os.name}")
//    private String osName;
//
//    @Value("${test.file}")
//    private String file;
    public static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        System.out.println("-------------------------------------");
//        System.out.println(testValue);
//        System.out.println(osName);
//        System.out.println(file);
//        System.out.println("-------------------------------------");
    }


}
