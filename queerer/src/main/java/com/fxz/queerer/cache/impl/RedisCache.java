package com.fxz.queerer.cache.impl;

import com.alibaba.fastjson.JSON;
import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.queerer.CacheOperate;
import com.fxz.queerer.util.CacheUtil;
import com.fxz.queerer.util.ConvertUtil;
import io.netty.handler.codec.dns.DnsRecordType;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 如果自行编码也可以借助RedisTemplate 编码能力
 * <p>
 * 自行编码的方式更加通用
 *
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

    @Trace
    @CatTracing
    @Override
    public List<BaseRecord> get(String host, String dnsRecordType) {
        ActiveSpan.tag("class", RedisCache.class.getName());
        ActiveSpan.tag("cache.host", host);
        ActiveSpan.tag("cache.type", dnsRecordType);
        String key = CacheUtil.assembleKey(host, dnsRecordType);
        ActiveSpan.tag("cache.key", key);
        Object o = redisTemplate.opsForValue().get(key);
        if (o != null) {
            ActiveSpan.tag("cache.result", o + "");
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

    @Trace
    @CatTracing
    @Override
    public Boolean set(String host, String dnsRecordType, List<BaseRecord> baseRecordList, Integer ttl) {
        ActiveSpan.tag("class", RedisCache.class.getName());
        ActiveSpan.tag("cache.host", host);
        ActiveSpan.tag("cache.type", dnsRecordType);
        if (baseRecordList != null && baseRecordList.size() > 0) {
            String key = CacheUtil.assembleKey(host, dnsRecordType);
            if (fixedTtl > 0) {
                ttl = Math.max(fixedTtl, ttl);
            }
            ActiveSpan.tag("cache.record.ttl", ttl + "");
            String value = JSON.toJSONString(baseRecordList);
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            ActiveSpan.tag("cache.record.result", value + "");
            return true;
        }
        return false;
    }
}
