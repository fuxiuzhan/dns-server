package com.fxz.queerer.util;

import com.fxz.dnscore.io.DatagramDnsResponse;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.processor.Processor;
import com.fxz.queerer.CacheOperate;
import com.fxz.queerer.cache.impl.LocalLRUCache;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author fxz
 */

public class CacheUtil {
    public static String assembleKey(String host, DnsRecordType dnsRecordType) {
        return dnsRecordType.name() + "_" + host.toLowerCase();
    }

    public static String assembleKey(String host, String dnsRecordType) {
        return dnsRecordType.toUpperCase() + "_" + host.toLowerCase();
    }

    public boolean putCache(Map<DnsRecordType, Processor> processorMap, CacheOperate cacheOperate, DatagramDnsResponse dnsResponse) {
        if (processorMap == null || processorMap.size() > 0) {
            return false;
        }
        if (cacheOperate == null) {
            throw new RuntimeException("cache component must be not null");
        }
        if (dnsResponse == null) {
            return false;
        }
        String key = assembleKey(dnsResponse.recordAt(DnsSection.QUESTION).name(), dnsResponse.recordAt(DnsSection.QUESTION).type());
        if (dnsResponse.count(DnsSection.ANSWER) > 0) {
            DnsRecord dnsRecord = dnsResponse.recordAt(DnsSection.ANSWER, 0);
            int ttl = (int) dnsRecord.timeToLive();
            Processor processor = processorMap.get(dnsResponse.recordAt(DnsSection.QUESTION).type());
            if (processor == null) {
                return false;
            }
            List<BaseRecord> baseRecordList = new ArrayList<>();
            for (int i = 0; i < dnsResponse.count(DnsSection.ANSWER); i++) {
                BaseRecord decode = processor.decode(dnsResponse.getRawData(), dnsResponse.recordAt(DnsSection.ANSWER, i));
                baseRecordList.add(decode);
            }
            LocalLRUCache.LocalCacheValue localCacheValue = new LocalLRUCache.LocalCacheValue();
            localCacheValue.setObject(baseRecordList);
            localCacheValue.setLastAccessTime(System.currentTimeMillis());
            localCacheValue.setExpr(ttl);
            localCacheValue.setTimeUnit(TimeUnit.SECONDS);
            cacheOperate.set(dnsResponse.recordAt(DnsSection.QUESTION).name(), dnsResponse.recordAt(DnsSection.QUESTION).type(), baseRecordList, ttl);
        }
        return false;
    }

    public static long convertUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case MINUTES:
                return 60L;
            case HOURS:
                return 60 * 60L;
            case DAYS:
                return 24 * 60 * 60L;
            default:
                return 1L;
        }
    }
}
