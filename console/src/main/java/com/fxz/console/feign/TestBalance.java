package com.fxz.console.feign;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import java.util.List;

/**
 * @author fxz
 */
public class TestBalance extends AbstractLoadBalancerRule implements ILoadBalancer {

    private String testrouteKey = "baidu";

    private ServerList<Server> serverListl;

    public TestBalance(ServerList serverList) {
        this.serverListl = serverList;
    }

    @Override
    public Server choose(Object key) {
        String defaultId = key == null ? testrouteKey : key.toString();
        return serverListl.getInitialListOfServers().stream().filter(s -> s.getMetaInfo().getInstanceId().equalsIgnoreCase(defaultId)).findFirst().get();
    }

    @Override
    public void addServers(List<Server> newServers) {
        System.out.println("addServer");
    }

    @Override
    public Server chooseServer(Object key) {
        return choose(key);
    }

    @Override
    public void markServerDown(Server server) {
        System.out.println("markServerDown");
    }

    @Override
    public List<Server> getServerList(boolean availableOnly) {
        return null;
    }

    @Override
    public List<Server> getReachableServers() {
        return null;
    }

    @Override
    public List<Server> getAllServers() {
        return null;
    }

    @Override
    public void initWithNiwsConfig(IClientConfig iClientConfig) {

    }
}
