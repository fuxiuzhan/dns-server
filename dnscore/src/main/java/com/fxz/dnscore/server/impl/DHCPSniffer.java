package com.fxz.dnscore.server.impl;


import com.fxz.dnscore.exporter.HostInfoExport;
import com.fxz.dnscore.server.LifeCycle;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dhcp4java.DHCPConstants;
import org.dhcp4java.DHCPPacket;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dhcp4java.DHCPConstants.*;

@Slf4j
public class DHCPSniffer implements LifeCycle {

    private static List<HostInfoExport> hostInfoExports;

    public DHCPSniffer(List<HostInfoExport> hostInfoExports) {
        this.hostInfoExports = hostInfoExports;
    }

    public static Map<String, HostInfo> hostInfoMap = new HashMap<>();

    @Override

    public String name() {
        return "DHCPSniffer";
    }

    @Override
    public void start() {
        new Thread(() -> DHCPSniffer.snifferStart()).start();
    }

    @Override
    public void stop() {

    }

    @Override
    public void restart() {
        throw new RuntimeException("operating not support");
    }

    /**
     *
     */
    public static void snifferStart() {
        log.info("DHCPSniffer Starting....");
        try {
            DatagramSocket socket = new DatagramSocket(DHCPConstants.BOOTP_REQUEST_PORT);
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1500], 1500);
                    socket.receive(receivePacket);
                    DHCPPacket resultDhcpPacket = DHCPPacket.getPacket(receivePacket);
                    log.info("dhcp packet->{}", resultDhcpPacket);
                    if (DHCPREQUEST == resultDhcpPacket.getDHCPMessageType()) {
                        String ip = resultDhcpPacket.getOptionAsInetAddr(DHO_DHCP_REQUESTED_ADDRESS).getHostAddress();
                        String mac = bytesToHexString(resultDhcpPacket.getOptionRaw(DHO_DHCP_CLIENT_IDENTIFIER)).substring(2).toUpperCase();
                        String hostName = mac;
                        try {
                            hostName = resultDhcpPacket.getOptionAsString(DHO_HOST_NAME);
                        } catch (Exception e) {
                            log.error("hdcp ->{}", e);
                        }
                        if (!StringUtils.isEmpty(hostName) && !StringUtils.isEmpty(ip) && !StringUtils.isEmpty(mac)) {
                            HostInfo hostInfo = new HostInfo();
                            hostInfo.setHostName(hostName);
                            hostInfo.setIp(ip);
                            hostInfo.setMac(mac);
                            hostInfoMap.put(ip, hostInfo);
                            if (!CollectionUtils.isEmpty(hostInfoExports)) {
                                try {
                                    for (HostInfoExport hostInfoExport : hostInfoExports) {
                                        hostInfoExport.export(hostInfo);
                                    }
                                } catch (Exception e) {
                                    log.error("hostInfoExport error->{}", e);
                                }
                            }
                            log.info("dhcp package receive ->{}", hostInfo);
                        }
                    }
                } catch (Exception e) {
                    log.error("HDCPSniffer error->{}", e);
                }
            }
        } catch (Exception e) {
            log.error("HDCPSniffer error->{}", e);
        }
    }

    /**
     * @param src
     * @return
     */
    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    @Data
    public static class HostInfo {
        private String ip;
        private String mac;
        private String hostName;

        @Override
        public String toString() {
            return "HostInfo{" +
                    "ip='" + ip + '\'' +
                    ", mac='" + mac + '\'' +
                    ", hostName='" + hostName + '\'' +
                    '}';
        }
    }
}
