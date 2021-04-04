package com.fxz.dnscore.io;

import io.netty.channel.AddressedEnvelope;
import io.netty.handler.codec.dns.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author xiuzhan.fu
 */
public class DatagramDnsResponse extends DefaultDnsResponse implements AddressedEnvelope<DatagramDnsResponse, InetSocketAddress> {
    private final InetSocketAddress sender;
    private final InetSocketAddress recipient;
    private byte[] rawData;

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public DatagramDnsResponse(InetSocketAddress sender, InetSocketAddress recipient, int id) {
        this(sender, recipient, id, DnsOpCode.QUERY, DnsResponseCode.NOERROR);
    }

    public DatagramDnsResponse(InetSocketAddress sender, InetSocketAddress recipient, int id, DnsOpCode opCode) {
        this(sender, recipient, id, opCode, DnsResponseCode.NOERROR);
    }

    public DatagramDnsResponse(InetSocketAddress sender, InetSocketAddress recipient, int id, DnsOpCode opCode, DnsResponseCode responseCode) {
        super(id, opCode, responseCode);
        if (recipient == null && sender == null) {
            throw new NullPointerException("recipient and sender");
        } else {
            this.sender = sender;
            this.recipient = recipient;
        }
    }

    @Override
    public DatagramDnsResponse content() {
        return this;
    }

    @Override
    public InetSocketAddress sender() {
        return this.sender;
    }

    @Override
    public InetSocketAddress recipient() {
        return this.recipient;
    }

    @Override
    public DatagramDnsResponse setAuthoritativeAnswer(boolean authoritativeAnswer) {
        return (DatagramDnsResponse) super.setAuthoritativeAnswer(authoritativeAnswer);
    }

    @Override
    public DatagramDnsResponse setTruncated(boolean truncated) {
        return (DatagramDnsResponse) super.setTruncated(truncated);
    }

    @Override
    public DatagramDnsResponse setRecursionAvailable(boolean recursionAvailable) {
        return (DatagramDnsResponse) super.setRecursionAvailable(recursionAvailable);
    }

    @Override
    public DatagramDnsResponse setCode(DnsResponseCode code) {
        return (DatagramDnsResponse) super.setCode(code);
    }

    @Override
    public DatagramDnsResponse setId(int id) {
        return (DatagramDnsResponse) super.setId(id);
    }

    @Override
    public DatagramDnsResponse setOpCode(DnsOpCode opCode) {
        return (DatagramDnsResponse) super.setOpCode(opCode);
    }

    @Override
    public DatagramDnsResponse setRecursionDesired(boolean recursionDesired) {
        return (DatagramDnsResponse) super.setRecursionDesired(recursionDesired);
    }

    @Override
    public DatagramDnsResponse setZ(int z) {
        return (DatagramDnsResponse) super.setZ(z);
    }

    @Override
    public DatagramDnsResponse setRecord(DnsSection section, DnsRecord record) {
        return (DatagramDnsResponse) super.setRecord(section, record);
    }

    @Override
    public DatagramDnsResponse addRecord(DnsSection section, DnsRecord record) {
        return (DatagramDnsResponse) super.addRecord(section, record);
    }

    @Override
    public DatagramDnsResponse addRecord(DnsSection section, int index, DnsRecord record) {
        return (DatagramDnsResponse) super.addRecord(section, index, record);
    }

    @Override
    public DatagramDnsResponse clear(DnsSection section) {
        return (DatagramDnsResponse) super.clear(section);
    }

    @Override
    public DatagramDnsResponse clear() {
        return (DatagramDnsResponse) super.clear();
    }

    @Override
    public DatagramDnsResponse touch() {
        return (DatagramDnsResponse) super.touch();
    }

    @Override
    public DatagramDnsResponse touch(Object hint) {
        return (DatagramDnsResponse) super.touch(hint);
    }

    @Override
    public DatagramDnsResponse retain() {
        return (DatagramDnsResponse) super.retain();
    }

    @Override
    public DatagramDnsResponse retain(int increment) {
        return (DatagramDnsResponse) super.retain(increment);
    }

    public DatagramDnsResponse copyResponse() {
        DatagramDnsResponse res = new DatagramDnsResponse(sender, recipient, id());
        res.setRawData(this.rawData);
        copySection(res, DnsSection.QUESTION);
        copySection(res, DnsSection.ANSWER);
        copySection(res, DnsSection.ADDITIONAL);
        copySection(res, DnsSection.AUTHORITY);
        return res;
    }

    private void copySection(DatagramDnsResponse newRes, DnsSection dnsSection) {
        int count = this.count(dnsSection);
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                newRes.addRecord(dnsSection, this.recordAt(dnsSection, i));
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!super.equals(obj)) {
            return false;
        } else if (!(obj instanceof AddressedEnvelope)) {
            return false;
        } else {
            AddressedEnvelope<?, SocketAddress> that = (AddressedEnvelope) obj;
            if (this.sender() == null) {
                if (that.sender() != null) {
                    return false;
                }
            } else if (!this.sender().equals(that.sender())) {
                return false;
            }

            if (this.recipient() == null) {
                if (that.recipient() != null) {
                    return false;
                }
            } else if (!this.recipient().equals(that.recipient())) {
                return false;
            }

            return true;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        if (this.sender() != null) {
            hashCode = hashCode * 31 + this.sender().hashCode();
        }

        if (this.recipient() != null) {
            hashCode = hashCode * 31 + this.recipient().hashCode();
        }

        return hashCode;
    }
}
