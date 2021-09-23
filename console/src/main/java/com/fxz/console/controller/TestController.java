package com.fxz.console.controller;


import com.fxz.console.cache.BatchCache;
import com.fxz.console.cache.Cache;
import com.fxz.console.cache.CacheOpTypeEnum;
import com.fxz.dnscore.annotation.Monitor;
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


    @Monitor
    @GetMapping("/cache2")
    @BatchCache(value = {@Cache(value = "com.fxz.dns:", key = "#id",condition = "{#id eq '100'}"),
            @Cache(value = "com.fxz.dns:q:", key = "#id", expr = 10, localTurbo = true),
            @Cache(value = "com.fxz.dns:s", key = "#id", opType = CacheOpTypeEnum.SAVE), @Cache(expr = 5)})
    public String testCache2(String id) throws InterruptedException {
        Thread.sleep(10);
        return System.currentTimeMillis() + "";
    }
}
