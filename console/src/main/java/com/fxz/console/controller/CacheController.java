package com.fxz.console.controller;

import com.fxz.dnscore.annotation.Monitor;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.queerer.CacheOperate;
import com.fxz.queerer.util.CacheUtil;
import com.fxz.queerer.util.ConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author fxz
 */
@RestController
@RequestMapping("/cache")
public class CacheController {

    @Autowired
    CacheOperate cacheOperate;

    @Autowired
    RedisTemplate redisTemplate;

    @PostMapping("/query")
    @Monitor
    public List<BaseRecord> query(String host, String type) {
        if (StringUtils.hasText(host) && StringUtils.hasText(type)) {
            List<BaseRecord> baseRecordList1 = cacheOperate.get(host, type.toUpperCase());
            return baseRecordList1;
        }
        return null;
    }

    @PostMapping("/del")
    @Monitor
    public Boolean del(String host, String type) {
        if (StringUtils.hasText(host) && StringUtils.hasText(type)) {
            String key = CacheUtil.assembleKey(host, type.toUpperCase());
            return redisTemplate.delete(key);
        }
        return false;
    }

    @PostMapping("/update")
    @Monitor
    public Boolean update(String host, String type, String recordList, Integer ttl) {
        if (StringUtils.hasText(host) && StringUtils.hasText(type) && StringUtils.hasText(recordList) && ttl != null && ttl > 0) {
            List<BaseRecord> baseRecordList = ConvertUtil.decodeBaseRecordFromString(recordList);
            return cacheOperate.set(host, type.toUpperCase(), baseRecordList, ttl);
        }
        return false;
    }
}
