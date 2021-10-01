package com.fxz.console.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author fxz
 */
@FeignClient(name = "baidu", decode404 = true,fallback= TestFeign.DefaultCallBack.class )
public interface TestFeign {

    @GetMapping("/")
    String homePage();

    @Component
    class DefaultCallBack implements TestFeign {

        @Override
        public String homePage() {
            return "defaultMessage";
        }
    }
}
