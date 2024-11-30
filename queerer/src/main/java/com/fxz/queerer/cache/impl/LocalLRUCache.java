package com.fxz.queerer.cache.impl;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.queerer.CacheOperate;
import com.fxz.queerer.util.CacheUtil;
import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author fxz
 */
public class LocalLRUCache implements CacheOperate {

    private boolean checkExpr = Boolean.TRUE;

    public LocalLRUCache(boolean checkExpr) {
        this.checkExpr = checkExpr;
    }

    LruCache lruCache = new LruCache(1024);

    @CatTracing
    @Override
    public List<BaseRecord> get(String host, String dnsRecordType) {
        String key = CacheUtil.assembleKey(host, dnsRecordType);
        Object o = lruCache.get(key);
        if (o != null && o instanceof LocalCacheValue) {
            LocalCacheValue localCacheValue = (LocalCacheValue) o;
            long exprMillis = localCacheValue.getExpr() * CacheUtil.convertUnit(localCacheValue.timeUnit) * 1000 + localCacheValue.lastAccessTime;
            if (checkExpr) {
                return (List<BaseRecord>) localCacheValue.object;
            } else {
                if (System.currentTimeMillis() - exprMillis < 0) {
                    return (List<BaseRecord>) localCacheValue.object;
                } else {
                    lruCache.remove(key);
                }
            }
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

    @CatTracing
    @Override
    public Boolean set(String host, String dnsRecordType, List<BaseRecord> baseRecordList, Integer ttl) {
        if (baseRecordList != null && baseRecordList.size() > 0) {
            LocalCacheValue localCacheValue = new LocalCacheValue();
            localCacheValue.object = baseRecordList;
            localCacheValue.lastAccessTime = System.currentTimeMillis();
            localCacheValue.expr = ttl;
            localCacheValue.timeUnit = TimeUnit.SECONDS;
            String key = CacheUtil.assembleKey(host, dnsRecordType);
            lruCache.put(key, localCacheValue);
            return true;
        }
        return false;
    }

    @Data
    public static class LocalCacheValue {
        private Object object;
        private long lastAccessTime;
        private TimeUnit timeUnit;
        private long expr;
    }

    static class LruCache extends LinkedHashMap {
        int size = 1024;

        LruCache(int size) {
            super(size);
            this.size = size;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > size;
        }
    }
}
