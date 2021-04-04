package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

/**
 * @author fxz
 */
@Data
public class AAAARecord extends BaseRecord {

    public AAAARecord() {
        setType(DnsRecordType.AAAA.name());
    }

    private String ipV6;
}
