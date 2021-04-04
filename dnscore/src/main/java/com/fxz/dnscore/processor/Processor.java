package com.fxz.dnscore.processor;

import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.objects.common.ProcessResult;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;

import java.util.List;

public interface Processor {
    /**
     * process query
     *
     * @param question
     * @return
     */
    ProcessResult process(DefaultDnsQuestion question);

    /**
     * @param dnsRecord
     * @return
     */
    BaseRecord decode(byte[] rawData,DnsRecord dnsRecord);

    /**
     * @return
     */
    DnsRecordType type();
}
