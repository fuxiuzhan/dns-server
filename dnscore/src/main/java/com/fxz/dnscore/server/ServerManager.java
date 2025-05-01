package com.fxz.dnscore.server;

import com.fxz.component.fuled.cat.starter.component.threadpool.CatTraceWrapper;
import com.fxz.dnscore.common.ThreadPoolConfig;
import com.fxz.dnscore.common.utils.SortUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author fxz
 */
@Slf4j
public class ServerManager {
    private List<LifeCycle> serverList;

    public void setServerList(List<LifeCycle> serverList) {
        this.serverList = SortUtil.sort(serverList);
    }

    public void startAllServers() {
        for (LifeCycle server : serverList) {
            ThreadPoolConfig.getExportThreadPool().execute(CatTraceWrapper.buildRunnable(() -> {
                try {
                    server.start();
                } catch (InterruptedException e) {
                    log.error("server start failed e->{}", e);
                }
            }, ThreadPoolConfig.EXPORT_THREAD_POOL));
        }
    }
}
