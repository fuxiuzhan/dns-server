package com.fxz.dnscore.exporter;

import com.fxz.component.fuled.cat.starter.component.threadpool.CatTraceWrapper;
import com.fxz.dnscore.common.ThreadPoolConfig;
import com.fxz.dnscore.common.utils.SortUtil;
import com.fxz.dnscore.objects.BaseRecord;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.apache.skywalking.apm.toolkit.trace.Trace;

import java.util.List;

/**
 * @author fxz
 */
@Slf4j
public class ExporterManager {
    private List<Exporter> exporterList;

    public ExporterManager(List<Exporter> exporterList) {
        this.exporterList = SortUtil.sort(exporterList);
    }

    @Trace
    public void export(ChannelHandlerContext ctx, DatagramDnsQuery query, DatagramDnsResponse response, List<BaseRecord> records) {
        ThreadPoolConfig.getExportThreadPool().execute(CatTraceWrapper.buildRunnable(RunnableWrapper.of(() -> {
            for (Exporter exporter : exporterList) {
                try {
                    exporter.export(ctx, query, response, records);
                } catch (Exception e) {
                    log.error("export error exportName->{},error->{}", exporter.name(), e);
                }
            }
            try {
                ReferenceCountUtil.release(query);
                ReferenceCountUtil.release(response);
            } catch (Exception e) {
                log.error("release error ->{}", e);
            }
        }), ThreadPoolConfig.EXPORT_THREAD_POOL));
    }
}
