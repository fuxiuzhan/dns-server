package com.fxz.dnscore.objects;

import io.netty.handler.codec.dns.DnsRecordType;
import lombok.Data;

import java.io.Serializable;

/**
 * @author xiuzhan.fu
 */
@Data
public class BaseRecord implements Serializable {
    private String host;
    private int ttl;
    //for deserialize
    private String type;
}
