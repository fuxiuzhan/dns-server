package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

/**
 * @author fxz
 */
@Data
public class SOARecord extends BaseRecord {
    public SOARecord() {
        setType(DnsRecordType.SOA.name());
    }

    private String serverName;
    private String authority;
    private Integer SerialNo;
    private Integer refreshInternal;
    private Integer retreyInternal;
    private Integer limit;
    private Integer iTTl;
}
