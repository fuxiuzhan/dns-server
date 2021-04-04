package com.fxz.queerer;

import com.fxz.dnscore.objects.BaseRecord;
import io.netty.handler.codec.dns.DnsRecordType;

import java.util.List;

/**
 * @author fxz
 */
public interface CacheOperate {
    /**
     * @param host
     * @param dnsRecordType
     * @return
     */
    default List<BaseRecord> get(String host, DnsRecordType dnsRecordType) {
        return null;
    }

    /**
     * @param host
     * @param dnsRecordType
     * @param baseRecordList
     * @param ttl
     * @return
     */
    default Boolean set(String host, DnsRecordType dnsRecordType, List<BaseRecord> baseRecordList, Integer ttl) {
        return false;
    }
}
