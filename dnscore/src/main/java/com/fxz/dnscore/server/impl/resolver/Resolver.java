package com.fxz.dnscore.server.impl.resolver;

import com.fxz.dnscore.io.DatagramDnsResponse;
import com.fxz.dnscore.server.impl.DnsClient;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import java.util.concurrent.FutureTask;

/**
 * @author fxz
 */
public interface Resolver {

    /**
     * future get
     *
     * @param query
     * @return
     */
    FutureTask<DatagramDnsResponse> resolve(DnsClient dnsClient, DatagramDnsQuery query);

    /**
     * sync
     *
     * @param query
     * @return
     */
    DatagramDnsResponse resolveSync(DnsClient dnsClient,  DatagramDnsQuery query);
}
