package com.fxz.dnscore.coder;

import com.fxz.dnscore.objects.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.util.NetUtil;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.util.StringUtils;

public class DnsRecordCoder {

    /**
     * A
     * AAAA
     * PTR
     * CNAME
     * MX
     * SOA
     * NS
     * SRV
     * TXT
     */

    public static DefaultDnsRawRecord assembleA(String host, int ttl, String ip) {
        return assembleBase(host, DnsRecordType.A, ttl, NetUtil.createByteArrayFromIpAddressString(ip));
    }

    @Trace
    public static ARecord decodeA(DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        ARecord aRecord = new ARecord();
        decodeBase(aRecord, dnsRecord);
        aRecord.setIpV4(NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(byteBuf)));
        return aRecord;
    }

    private static void decodeBase(BaseRecord baseRecord, DnsRecord dnsRecord) {
        baseRecord.setTtl((int) dnsRecord.timeToLive());
        baseRecord.setHost(dnsRecord.name());
    }

    public static DefaultDnsRawRecord assembleAAAA(String host, int ttl, String ip) {
        return assembleBase(host, DnsRecordType.AAAA, ttl, NetUtil.createByteArrayFromIpAddressString(ip));
    }

    public static AAAARecord decodeAAAA(DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        AAAARecord aaaaRecord = new AAAARecord();
        decodeBase(aaaaRecord, dnsRecord);
        aaaaRecord.setIpV6(NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(byteBuf)));
        return aaaaRecord;
    }

    public static DefaultDnsRawRecord assemblePTR(String host, int ttl, String serverName) {
        ByteBuf assemble = assemble(serverName);
        byte[] buffer = new byte[assemble.readableBytes()];
        assemble.readBytes(buffer);
        return assembleBase(host, DnsRecordType.PTR, ttl, buffer);
    }

    public static PTRRecord decodePTR(DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        PTRRecord ptrRecord = new PTRRecord();
        decodeBase(ptrRecord, dnsRecord);
        ptrRecord.setPtr(decodeSingle(buffer));
        return ptrRecord;
    }

    public static DefaultDnsRawRecord assembleCNAME(String host, int ttl, String serverName) {
        ByteBuf assemble = assemble(serverName);
        byte[] buffer = new byte[assemble.readableBytes()];
        assemble.readBytes(buffer);
        return assembleBase(host, DnsRecordType.CNAME, ttl, buffer);
    }

    public static CNAMERecord decodeCNAME(byte[] rawData, DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        CNAMERecord cnameRecord = new CNAMERecord();
        decodeBase(cnameRecord, dnsRecord);
        cnameRecord.setCName(decodePtr(rawData, buffer));
        return cnameRecord;
    }

    public static DefaultDnsRawRecord assembleMX(String host, int ttl, String server, int wight) {
        byte[] textBuffer = host.getBytes();
        if (textBuffer.length > 127) {
            throw new RuntimeException("data too long");
        }
        ByteBuf assemble = Unpooled.buffer(textBuffer.length + 1);
        assemble.writeShort(wight);
        assemble.writeBytes(assemble(host));
        return assembleBase(host, DnsRecordType.MX, ttl, assemble);
    }

    public static MXRecord decodeMX(DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        int wight = byteBuf.readShort();
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        String host = decodeSingle(buffer);
        MXRecord mxRecord = new MXRecord();
        decodeBase(mxRecord, dnsRecord);
        mxRecord.setPriority(wight);
        mxRecord.setExchager(host);
        return mxRecord;
    }

    public static SOARecord decodeSOA(byte[] rawData, DnsRecord dnsRecord) {
        ByteBuf content = ((DnsRawRecord) dnsRecord).content();
        String serverName = decodeSingle(rawData, content);
        String authority = decodeSingle(rawData, content);
        int serialNo = content.readInt();
        int refreshInternal = content.readInt();
        int retryInter = content.readInt();
        int limit = content.readInt();
        int ttl = content.readInt();
        SOARecord soaRecord = new SOARecord();
        decodeBase(soaRecord, dnsRecord);
        soaRecord.setAuthority(authority);
        soaRecord.setServerName(serverName);
        soaRecord.setITTl(ttl);
        soaRecord.setLimit(limit);
        soaRecord.setRefreshInternal(refreshInternal);
        soaRecord.setRetreyInternal(retryInter);
        soaRecord.setSerialNo(serialNo);
        return soaRecord;
    }

    public static DefaultDnsRawRecord assembleSOA(String host, int ottl, String serverName, String authority, int serialNo, int refreshInternal, int retryInter, int limit, int ittl) {
        //assemble primary server name
        ByteBuf byteBufServerName = assemble(serverName);
        //assemble authority
        ByteBuf byteBufAuthority = assemble(authority);
        ByteBuf buf = Unpooled.buffer(5 * 4 + byteBufAuthority.readableBytes() + byteBufServerName.readableBytes());
        //push serverName
        byte[] bufferServerName = new byte[byteBufServerName.readableBytes()];
        byteBufServerName.readBytes(bufferServerName);
        buf.writeBytes(bufferServerName);
        //push auth
        byte[] bufferAuth = new byte[byteBufAuthority.readableBytes()];
        byteBufAuthority.readBytes(bufferAuth);
        buf.writeBytes(bufferAuth);
        buf.writeInt(serialNo);
        buf.writeInt(refreshInternal);
        buf.writeInt(retryInter);
        buf.writeInt(limit);
        buf.writeInt(ittl);
        return assembleBase(host, DnsRecordType.SOA, ottl, buf);
    }

    public static DefaultDnsRawRecord assembleNS(String host, int ttl, String serverName) {
        ByteBuf assemble = assemble(serverName);
        byte[] buffer = new byte[assemble.readableBytes()];
        assemble.readBytes(buffer);
        return assembleBase(host, DnsRecordType.NS, ttl, buffer);
    }

    public static NSRecord decodeNS(DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        NSRecord nsRecord = new NSRecord();
        decodeBase(nsRecord, dnsRecord);
        nsRecord.setDomainName(decodeSingle(buffer));
        return nsRecord;
    }

    public static SRVRecord decodeSRV(DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        int priority = byteBuf.readShort();
        int wight = byteBuf.readShort();
        int port = byteBuf.readShort();
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        String host = decodeSingle(buffer);
        SRVRecord srvRecord = new SRVRecord();
        decodeBase(srvRecord, dnsRecord);
        srvRecord.setPriority(priority);
        srvRecord.setWight(wight);
        srvRecord.setPort(port);
        srvRecord.setServer(host);
        return srvRecord;
    }

    public static DefaultDnsRawRecord assembleSRV(String host, int ttl, int priority, int wight, int port) {
        ByteBuf assemble = assemble(host);
        ByteBuf byteBuf = Unpooled.buffer(assemble.readableBytes() + 6);
        byteBuf.writeShort(priority);
        byteBuf.writeShort(wight);
        byteBuf.writeShort(port);
        byte[] buffer = new byte[assemble.readableBytes()];
        assemble.readBytes(buffer);
        byteBuf.writeBytes(buffer);
        return assembleBase(host, DnsRecordType.SRV, ttl, byteBuf);
    }

    public static TXTRecord decodeTXT(DnsRecord dnsRecord) {
        ByteBuf byteBuf = ((DnsRawRecord) dnsRecord).content();
        int length = byteBuf.readByte();
        byte[] buffer = new byte[length];
        byteBuf.readBytes(buffer);
        TXTRecord txtRecord = new TXTRecord();
        decodeBase(txtRecord, dnsRecord);
        txtRecord.setTxt(new String(buffer));
        return txtRecord;
    }

    public static DefaultDnsRawRecord assembleTXT(String host, int ttl, String txt) {
        byte[] textBuffer = txt.getBytes();
        if (textBuffer.length > 127) {
            throw new RuntimeException("data too long");
        }
        ByteBuf assemble = Unpooled.buffer(textBuffer.length + 1);
        assemble.writeByte(textBuffer.length);
        assemble.writeBytes(txt.getBytes());
        return assembleBase(host, DnsRecordType.TXT, ttl, assemble);
    }

    public static DefaultDnsRawRecord assembleBase(String host, DnsRecordType type, int ttl, byte[] context) {
        DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(host, type, ttl, Unpooled.wrappedBuffer(context));
        return queryAnswer;
    }

    public static DefaultDnsRawRecord assembleBase(String host, DnsRecordType type, int ttl, ByteBuf context) {
        DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(host, type, ttl, context);
        return queryAnswer;
    }

    public static String decodeSingle(byte[] rawData, ByteBuf content) {
        ByteBuf buffer = Unpooled.buffer(content.readableBytes());
        int trigger = 0;
        for (; ; ) {
            byte b = content.readByte();
            if (b != 0) {
                buffer.writeByte(b);
            } else {
                break;
            }
            if (trigger > 0) {
                break;
            }
            if (b < 0) {
                trigger++;
            }

        }
        byte[] tmpBuffer = new byte[buffer.readableBytes()];
        buffer.readBytes(tmpBuffer);
        String targetName = null;
        if (trigger > 0) {
            ByteBuf resultBuffer = Unpooled.buffer(rawData.length);
            targetName = decodeSingle(decodePtr(resultBuffer, rawData, tmpBuffer).getBytes());
        } else {
            targetName = decodeSingle(tmpBuffer);
        }
        return targetName;
    }

    public static String decodePtr(byte[] rawData, byte[] buffer) {
        ByteBuf resultBuffer = Unpooled.buffer(rawData.length);
        return decodeSingle(decodePtr(resultBuffer, rawData, buffer).getBytes());
    }

    private static String decodePtr(ByteBuf resultBuffer, byte[] rawData, byte[] buffer) {
        StringBuffer sb = new StringBuffer();
        int offset = 0;
        for (int i = 0; i < buffer.length; i++) {
            byte b = buffer[i];
            if (b > 0) {
                resultBuffer.writeByte(b);
            }
            if (b < 0) {
                offset = buffer[i + 1];
                break;
            }
            if (b == 0) {
                break;
            }
        }
        if (offset > 0) {
            //rawData offset=4
            offset -= 4;
            return decodePtr(resultBuffer, rawData, subBytes(rawData, offset, rawData.length - offset));
        }
        if (resultBuffer.readableBytes() > 0) {
            byte[] prefix = new byte[resultBuffer.readableBytes()];
            resultBuffer.readBytes(prefix);
            sb.append(new String(prefix));
        }
        //sb.append(decode(bytes));
        return sb.toString();
    }

    private static byte[] subBytes(byte[] buffer, int pos, int length) {
        byte[] rez = new byte[length];
        System.arraycopy(buffer, pos, rez, 0, length);
        return rez;
    }

    public static String decodeSingle(byte[] buffer) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
        byte b = byteBuf.readByte();
        StringBuilder sb = new StringBuilder();
        while (b != 0) {
            int length = b;
            byte[] tmpBuffer = new byte[length];
            byteBuf.readBytes(tmpBuffer);
            sb.append(new String(tmpBuffer));
            sb.append(".");
            if (byteBuf.readableBytes() > 0) {
                b = byteBuf.readByte();
            } else {
                b = 0;
            }
        }
        return sb.toString();
    }

    public static ByteBuf assemble(String name) {
        if (!StringUtils.hasText(name)) {
            throw new RuntimeException("context must be not null");
        }
        ByteBuf byteBuf = Unpooled.buffer(name.length() + 1);
        for (String str : name.split("\\.")) {
            byteBuf.writeByte(str.length());
            byteBuf.writeBytes(str.getBytes());
        }
        byteBuf.writeByte(0);
        return byteBuf;
    }

}
