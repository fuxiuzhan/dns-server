package com.fxz.starter.config;


import com.fxz.dnscore.MainProcessor;
import com.fxz.dnscore.common.ThreadPoolConfig;
import com.fxz.dnscore.exporter.Exporter;
import com.fxz.dnscore.exporter.ExporterManager;
import com.fxz.dnscore.exporter.HostInfoExport;
import com.fxz.dnscore.processor.Processor;
import com.fxz.dnscore.processor.ProcessorManger;
import com.fxz.dnscore.processor.impl.*;
import com.fxz.dnscore.queerer.QueryManger;
import com.fxz.dnscore.server.LifeCycle;
import com.fxz.dnscore.server.ServerManager;
import com.fxz.dnscore.server.impl.DHCPSniffer;
import com.fxz.dnscore.server.impl.DnsClient;
import com.fxz.dnscore.server.impl.DnsServer;
import com.fxz.dnscore.server.impl.handler.ServerHandler;
import com.fxz.exporter.DefaultExporter;
import com.fxz.exporter.elastic.baserepository.BaseRecordRepository;
import com.fxz.exporter.elastic.baserepository.BaseSourceRepository;
import com.fxz.fuled.common.chain.FilterChainManger;
import com.fxz.queerer.CacheOperate;
import com.fxz.queerer.cache.impl.LocalLRUCache;
import com.fxz.queerer.cache.impl.RedisCache;
import com.fxz.queerer.host.impl.RedisHostInfoExport;
import com.fxz.queerer.query.impl.*;
import com.fxz.queerer.resolver.impl.ParentResolver;
import com.fxz.starter.exporter.EsExporter;
import com.fxz.starter.queerer.EsQueryFilter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xiuzhan.fu
 */
@Component
@Import({RegexBlackHostFilter.class, BlackHostFilter.class, BeforeQueryFilter.class, CacheQueryFilter.class, PtrQueryFilter.class, EsQueryFilter.class, FilterChainManger.class})
@Slf4j
public class AutoConfig {

    @Value("${dns.server.ip:}")
    private String ip;
    @Value("${dns.server.port:53}")
    private Integer port;
    @Value("${dns.domain.servers:114.114.114.114}")
//    @Value("#{'${dns.domain.servers}'.split(',')}")
    private String domainServers;

    @Value("${spring.application.name:fuled_dns_server}")
    private String dnsServerName;
    @Value("${dns.query.cache.expr.enabled:true}")
    private boolean checkCacheExpr;


    @Bean
    public QueryManger injectQueryManger() {
        QueryManger queryManger = new QueryManger();
        return queryManger;
    }

    @Bean
    @ConditionalOnMissingBean(DnsClient.class)
    public DnsClient injectDefaultClient() {
        DnsClient dnsClient = new DnsClient();
        return dnsClient;
    }

    @Bean
    public ParentResolver injectParentResolver(@Autowired DnsClient dnsClient) {
        ParentResolver parentResolver = new ParentResolver();
        parentResolver.setDnsClient(dnsClient);
        List<String> domainServerList = Arrays.stream(domainServers.split(",")).filter(a -> StringUtils.hasText(a)).collect(Collectors.toList());
        parentResolver.setDomainServers(domainServerList);
        return parentResolver;
    }


    @Bean
    public CacheOperate injectCache(@Autowired(required = false) RedisTemplate redisTemplate) {
        if (redisTemplate == null) {
            return new LocalLRUCache(checkCacheExpr);
        } else {
            redisTemplate.setKeySerializer(new GenericToStringSerializer(String.class));
            redisTemplate.setValueSerializer(new GenericToStringSerializer(String.class));
            return new RedisCache(redisTemplate);
        }
    }

    @Bean
    public DefaultExporter injectDefaultExporter() {
        return new DefaultExporter();
    }

    @Bean
    @ConditionalOnClass({BaseRecordRepository.class, BaseSourceRepository.class})
    public EsExporter injectEsExporter(@Autowired BaseRecordRepository recordRepository, @Autowired BaseSourceRepository sourceRepository, @Autowired RestHighLevelClient highLevelClient, @Autowired StringRedisTemplate stringRedisTemplate) {
        return new EsExporter(recordRepository, sourceRepository, dnsServerName, highLevelClient, stringRedisTemplate);
    }

    @Bean
    public List<Processor> injectProcessorList(@Autowired QueryManger queryManger) {
        List<Processor> processorList = new ArrayList<>();
        processorList.add(new AProcessor(queryManger));
        processorList.add(new AAAAProcessor(queryManger));
        processorList.add(new SOAProcessor(queryManger));
        processorList.add(new CNAMEProcessor(queryManger));
        processorList.add(new MXProcessor(queryManger));
        processorList.add(new NSProcessor(queryManger));
        processorList.add(new PTRProcessor(queryManger));
        processorList.add(new SRVProcessor(queryManger));
        processorList.add(new TXTProcessor(queryManger));
        return processorList;
    }

    @Bean
    public LifeCycle injectDefaultServer(@Autowired ServerHandler serverHandler) {
        DnsServer dnsServer = new DnsServer();
        dnsServer.setHandler(serverHandler);
        dnsServer.setIp(ip);
        dnsServer.setPort(port);
        return dnsServer;
    }

    @Bean
    public HostInfoExport redisHostInfoExport() {
        return new RedisHostInfoExport();
    }

    @Bean
    public LifeCycle injectDefaultDHCPSniffer(@Autowired(required = false) List<HostInfoExport> hostInfoExports) {
        return new DHCPSniffer(hostInfoExports);
    }


    @Bean
    @ConditionalOnMissingBean(ServerManager.class)
    public ServerManager injectServerManager(@Autowired List<LifeCycle> serverList) {
        ServerManager serverManager = new ServerManager();
        serverManager.setServerList(serverList);
        serverManager.startAllServers();
        return serverManager;
    }

    @Bean("serverHandler")
    @ConditionalOnMissingBean(ServerHandler.class)
    public ServerHandler injectHandler(@Autowired MainProcessor mainProcessor) {
        ServerHandler serverHandler = new ServerHandler();
        serverHandler.setMainProcessor(mainProcessor);
        return serverHandler;
    }

    @Bean
    @ConditionalOnMissingBean(MainProcessor.class)
    public MainProcessor injectMainProcessor(@Autowired ExporterManager exporterManager, @Autowired ProcessorManger processorManger) {
        MainProcessor mainProcessor = new MainProcessor();
        mainProcessor.setExporterManager(exporterManager);
        mainProcessor.setProcessorManger(processorManger);
        return mainProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(ExporterManager.class)
    public ExporterManager injectExporterManager(@Autowired List<Exporter> exporterList) {
        ExporterManager exporterManager = new ExporterManager(exporterList);
        return exporterManager;
    }

    @Bean
    @ConditionalOnMissingBean(ProcessorManger.class)
    public ProcessorManger injectProcessorManger(@Autowired List<Processor> processorList, @Autowired ParentResolver parentResolver) {
        ProcessorManger processorManger = new ProcessorManger();
        processorManger.setProcessorList(processorList);
        parentResolver.setProcessorMap(processorManger.getProcessorMap());
        return processorManger;
    }


    @Bean
    public ThreadPoolConfig threadPoolConfig() {
        return new ThreadPoolConfig();
    }

    @Bean
    public TypeConfig typeConfig() {
        return new TypeConfig();
    }

    @Bean
    public Ip2RegionConfig ip2RegionConfig() {
        return new Ip2RegionConfig();
    }
}
