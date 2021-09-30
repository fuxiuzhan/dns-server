package com.fxz.console.feign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author fxz
 */
@Component
public class CommandLine implements ApplicationRunner {

    @Autowired
    TestFeign testFeign;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println(testFeign.homePage());
        System.out.println(testFeign.homePage());
    }
}
