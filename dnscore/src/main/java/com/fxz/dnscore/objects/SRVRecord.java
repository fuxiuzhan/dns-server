package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

@Data
public class SRVRecord extends BaseRecord {
    public SRVRecord() {
        setType(DnsRecordType.SRV.name());
    }

    private Integer priority;
    private Integer wight;
    private Integer port;
    private String server;
}
