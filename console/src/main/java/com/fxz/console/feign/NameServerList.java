package com.fxz.console.feign;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import java.util.Arrays;
import java.util.List;

public class NameServerList implements ServerList {
    @Override
    public List<Server> getInitialListOfServers() {
        Server server = new TestServer("192.168.10.201", 9200);
        return Arrays.asList(server);
    }

    @Override
    public List<Server> getUpdatedListOfServers() {
        Server server = new TestServer("192.168.10.201", 9200);
        return Arrays.asList(server);
    }
}
