package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

@Data
public class PTRRecord extends BaseRecord {
    public PTRRecord() {
        setType(DnsRecordType.PTR.name());
    }

    private String ptr;
}
