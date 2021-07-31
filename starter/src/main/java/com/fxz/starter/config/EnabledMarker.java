package com.fxz.starter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fxz
 */
@Configuration
public class EnabledMarker {

    @Bean
    public MarkClient makeClient() {
        return new MarkClient();
    }

    public class MarkClient {

    }
}
