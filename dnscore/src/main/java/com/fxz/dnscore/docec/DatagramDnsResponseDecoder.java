package com.fxz.dnscore.docec;

import com.fxz.dnscore.io.DatagramDnsResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.dns.*;
import io.netty.util.internal.ObjectUtil;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author xiuzhan.fu
 */
public class DatagramDnsResponseDecoder extends MessageToMessageDecoder<DatagramPacket> {
    private final DnsRecordDecoder recordDecoder;

    public DatagramDnsResponseDecoder() {
        this(DnsRecordDecoder.DEFAULT);
    }

    public DatagramDnsResponseDecoder(DnsRecordDecoder recordDecoder) {
        this.recordDecoder = ObjectUtil.checkNotNull(recordDecoder, "recordDecoder");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
        ByteBuf buf = packet.content();
        DnsResponse response = newResponse(packet, buf);
        boolean success = false;

        try {
            byte[] buffer = new byte[buf.readableBytes()];
            buf.copy().readBytes(buffer);
            ((com.fxz.dnscore.io.DatagramDnsResponse) response).setRawData(buffer);
            int questionCount = buf.readUnsignedShort();
            int answerCount = buf.readUnsignedShort();
            int authorityRecordCount = buf.readUnsignedShort();
            int additionalRecordCount = buf.readUnsignedShort();
            this.decodeQuestions(response, buf, questionCount);
            this.decodeRecords(response, DnsSection.ANSWER, buf, answerCount);
            this.decodeRecords(response, DnsSection.AUTHORITY, buf, authorityRecordCount);
            this.decodeRecords(response, DnsSection.ADDITIONAL, buf, additionalRecordCount);

            out.add(response);
            success = true;
        } finally {
            if (!success) {
                response.release();
            }

        }

    }

    private static DnsResponse newResponse(DatagramPacket packet, ByteBuf buf) {
        int id = buf.readUnsignedShort();
        int flags = buf.readUnsignedShort();
        if (flags >> 15 == 0) {
            throw new CorruptedFrameException("not a response");
        } else {
            DnsResponse response = new DatagramDnsResponse((InetSocketAddress) packet.sender(), (InetSocketAddress) packet.recipient(), id, DnsOpCode.valueOf((byte) (flags >> 11 & 15)), DnsResponseCode.valueOf((byte) (flags & 15)));
            response.setRecursionDesired((flags >> 8 & 1) == 1);
            response.setAuthoritativeAnswer((flags >> 10 & 1) == 1);
            response.setTruncated((flags >> 9 & 1) == 1);
            response.setRecursionAvailable((flags >> 7 & 1) == 1);
            response.setZ(flags >> 4 & 7);
            return response;
        }
    }

    private void decodeQuestions(DnsResponse response, ByteBuf buf, int questionCount) throws Exception {
        for (int i = questionCount; i > 0; --i) {
            response.addRecord(DnsSection.QUESTION, this.recordDecoder.decodeQuestion(buf));
        }

    }

    private void decodeRecords(DnsResponse response, DnsSection section, ByteBuf buf, int count) throws Exception {
        for (int i = count; i > 0; --i) {
            DnsRecord r = this.recordDecoder.decodeRecord(buf);
            if (r == null) {
                break;
            }

            response.addRecord(section, r);
        }

    }
}
