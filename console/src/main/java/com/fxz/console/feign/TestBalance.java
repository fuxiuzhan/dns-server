package com.fxz.console.feign;

import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import java.util.List;

/**
 * @author fxz
 */
public class TestBalance extends AbstractLoadBalancerRule implements ILoadBalancer {
    @Override
    public Server choose(Object key) {
        return getLoadBalancer().chooseServer(key);
    }

    @Override
    public void addServers(List<Server> newServers) {
        System.out.println("addServer");
    }

    @Override
    public Server chooseServer(Object key) {
        System.out.println("chooseServer");
        return null;
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
}
