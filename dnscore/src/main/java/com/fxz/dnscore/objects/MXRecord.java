package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

@Data
public class MXRecord extends BaseRecord {
    public MXRecord() {
        setType(DnsRecordType.MX.name());
    }

    private Integer priority;
    private String exchager;
}
