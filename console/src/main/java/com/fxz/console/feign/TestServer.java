package com.fxz.console.feign;

import com.netflix.loadbalancer.Server;

/**
 * @author fxz
 */
public class TestServer extends Server {
    private MetaInfo metaInfo;

    public TestServer(String host, int port) {
        super(host, port);
        this.metaInfo = new TestMeta();
    }

    @Override
    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    class TestMeta implements MetaInfo {

        @Override
        public String getAppName() {
            return "baidu";
        }

        @Override
        public String getServerGroup() {
            return "baidu";
        }

        @Override
        public String getServiceIdForDiscovery() {
            return "baidu";
        }

        @Override
        public String getInstanceId() {
            return "baidu";
        }
    }
}
