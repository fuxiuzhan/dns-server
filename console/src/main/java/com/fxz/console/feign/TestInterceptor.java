package com.fxz.console.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.UUID;

/**
 * @author fxz
 */
public class TestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        System.out.println("requestTemplate->" + requestTemplate);
        requestTemplate.header("_traceId", UUID.randomUUID().toString());
    }
}
