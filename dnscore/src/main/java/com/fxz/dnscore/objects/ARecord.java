package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

/**
 * @author xiuzhan.fu
 */
@Data
public class ARecord extends BaseRecord {
    public ARecord(){
        setType(DnsRecordType.A.name());
    }
    private String ipV4;
}
