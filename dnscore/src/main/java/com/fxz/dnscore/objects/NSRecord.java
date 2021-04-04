package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

@Data
public class NSRecord extends BaseRecord {
    public NSRecord() {
        setType(DnsRecordType.NS.name());
    }

    private String domainName;
}
