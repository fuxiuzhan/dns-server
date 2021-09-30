package com.fxz.console.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author fxz
 */
@FeignClient(name = "baidu")
public interface TestFeign {

    @GetMapping("/")
    String homePage();
}
