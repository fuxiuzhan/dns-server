package com.fxz.starter.config;

import com.fxz.fuled.common.utils.ReflectionUtil;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Map;

@Slf4j
public class TypeConfig {

    @PostConstruct
    public void init() throws IllegalAccessException {
        addType(new DnsRecordType(65, "HTTPS"));
    }

    /**
     * @param dnsRecordType
     */
    public void addType(DnsRecordType dnsRecordType) throws IllegalAccessException {
        Field byName = ReflectionUtil.getField(DnsRecordType.class, "BY_NAME");
        if (!byName.isAccessible()) {
            byName.setAccessible(Boolean.TRUE);
        }
        Map<String, DnsRecordType> byNameMap = (Map<String, DnsRecordType>) byName.get(DnsRecordType.class);
        byNameMap.put("HTTPS", dnsRecordType);

        Field byType = ReflectionUtil.getField(DnsRecordType.class, "BY_TYPE");
        if (!byType.isAccessible()) {
            byType.setAccessible(Boolean.TRUE);
        }
        IntObjectHashMap<DnsRecordType> intObjectHashMap = (IntObjectHashMap<DnsRecordType>) byType.get(DnsRecordType.class);
        intObjectHashMap.put(dnsRecordType.intValue(), dnsRecordType);
        log.info("add new Type->{}", dnsRecordType);
    }

}
