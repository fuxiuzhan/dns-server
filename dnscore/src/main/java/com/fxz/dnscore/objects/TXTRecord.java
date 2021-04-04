package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

/**
 * @author fxz
 */
@Data
public class TXTRecord extends BaseRecord {
    public TXTRecord() {
        setType(DnsRecordType.TXT.name());
    }

    private String txt;
}
