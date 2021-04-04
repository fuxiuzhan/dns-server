package com.fxz.dnscore.objects.common;

import com.fxz.dnscore.objects.BaseRecord;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import lombok.Data;

import java.util.List;

/**
 * @author fxz
 */
@Data
public class ProcessResult {
    private List<DefaultDnsRawRecord> rawRecords;
    private List<BaseRecord> records;
}
