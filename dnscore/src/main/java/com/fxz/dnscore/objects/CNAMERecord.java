package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

/**
 * @author xiuzhan.fu
 */
@Data
public class CNAMERecord extends BaseRecord {
    public CNAMERecord() {
        setType(DnsRecordType.CNAME.name());
    }

    private String cName;
}
