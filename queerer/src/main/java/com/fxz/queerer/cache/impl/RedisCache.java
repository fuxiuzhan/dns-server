package com.fxz.queerer.cache.impl;

import com.alibaba.fastjson.JSON;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.queerer.CacheOperate;
import com.fxz.queerer.util.CacheUtil;
import com.fxz.queerer.util.ConvertUtil;
import io.netty.handler.codec.dns.DnsRecordType;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author fxz
 */
public class RedisCache implements CacheOperate {
    RedisTemplate redisTemplate;
    private int fixedTtl;

    public RedisCache(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RedisCache(RedisTemplate redisTemplate, int fixedTtl) {
        this.redisTemplate = redisTemplate;
        this.fixedTtl = fixedTtl;
    }

    @Override
    public List<BaseRecord> get(String host, String dnsRecordType) {
        String key = CacheUtil.assembleKey(host, dnsRecordType);
        Object o = redisTemplate.opsForValue().get(key);
        if (o != null) {
            return ConvertUtil.decodeBaseRecordFromString((String) o);
        }
        return null;
    }

    @Override
    public List<BaseRecord> get(String host, DnsRecordType dnsRecordType) {
        return get(host, dnsRecordType.name());
    }

    @Override
    public Boolean set(String host, DnsRecordType dnsRecordType, List<BaseRecord> baseRecordList, Integer ttl) {
        return set(host, dnsRecordType.name(), baseRecordList, ttl);
    }

    @Override
    public Boolean set(String host, String dnsRecordType, List<BaseRecord> baseRecordList, Integer ttl) {
        if (baseRecordList != null && baseRecordList.size() > 0) {
            String key = CacheUtil.assembleKey(host, dnsRecordType);
            if (fixedTtl > 0) {
                ttl = fixedTtl;
            }
            redisTemplate.opsForValue().set(key, JSON.toJSONString(baseRecordList), ttl, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }
}
