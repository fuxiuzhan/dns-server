package com.fxz.starter.starter;

import com.fxz.dnscore.server.ServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author fxz
 */
@Component
public class ServerStarter implements ApplicationRunner {
    @Autowired
    ServerManager serverManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        serverManager.startAllServers();
    }
}
