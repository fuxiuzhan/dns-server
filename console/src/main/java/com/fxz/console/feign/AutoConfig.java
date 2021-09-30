package com.fxz.console.feign;

import com.netflix.loadbalancer.PollingServerListUpdater;
import com.netflix.loadbalancer.ServerList;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class AutoConfig {
    @Bean
    public ServerList<?> ribbonServerList() {
        return new NameServerList();
    }

    @Bean
    public DiscoveryClient defaultClient(ServerList serverList) {
        return new TestDiscoveryClient(serverList);
    }

    @Bean
    public PollingServerListUpdater updater(ServerList serverList) {
        PollingServerListUpdater updater = new PollingServerListUpdater();
        updater.start(() -> {
            serverList.getUpdatedListOfServers();
        });
        return updater;
    }
}
