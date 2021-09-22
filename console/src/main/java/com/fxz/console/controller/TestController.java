package com.fxz.console.controller;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fxz
 */
@RestController("/test")
public class TestController {


    @GetMapping("/cache")
    @Caching(cacheable = {@Cacheable(value = "user:id:", key = "#id", cacheManager = "apiMgr")
            , @Cacheable(value = "user:id:", cacheManager = "apiMgr"),
            @Cacheable(value = "h", key = "'hour'+#id"), @Cacheable(value = "m", key = "'mins'+#id")}
            , evict = {@CacheEvict(value = "user:id2:", key = "'test'+#id")})
    public String testCache(String id) {
        return System.currentTimeMillis() + "";
    }
}
