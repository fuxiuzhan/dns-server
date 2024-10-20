package com.fxz.dnscore.exporter;

import com.fxz.dnscore.server.impl.DHCPSniffer;

/**
 *
 */
public interface HostInfoExport {

    /**
     *
     * @param hostInfo
     */
    default void export(DHCPSniffer.HostInfo hostInfo) {

    }
}
