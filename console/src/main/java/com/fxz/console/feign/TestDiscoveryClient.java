package com.fxz.console.feign;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author fxz
 */
public class TestDiscoveryClient implements DiscoveryClient {
    ServerList serverList;

    public TestDiscoveryClient(ServerList serverList) {
        this.serverList = serverList;
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        List<Server> initialListOfServers = serverList.getInitialListOfServers();

        if (initialListOfServers != null && initialListOfServers.size() > 0) {
            initialListOfServers.forEach(s -> {
                serviceInstances.add(new ServiceInstance() {

                    @Override
                    public String getServiceId() {
                        return s.getMetaInfo().getInstanceId();
                    }

                    @Override
                    public String getHost() {
                        return s.getHost();
                    }

                    @Override
                    public int getPort() {
                        return s.getPort();
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public URI getUri() {
                        return null;
                    }

                    @Override
                    public Map<String, String> getMetadata() {
                        return null;
                    }
                });
            });
        }
        return serviceInstances;
    }

    @Override
    public List<String> getServices() {
        List<String> nameList = new ArrayList<>();
        List<Server> updatedListOfServers = serverList.getUpdatedListOfServers();
        if (updatedListOfServers != null && updatedListOfServers.size() > 0) {
            updatedListOfServers.forEach(s -> {
                nameList.add(s.getMetaInfo().getInstanceId());
            });
        }
        return nameList;
    }
}
