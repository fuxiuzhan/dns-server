package com.fxz.queerer.query.impl;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.fuled.common.chain.Filter;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.common.chain.annotation.FilterProperty;
import com.fxz.fuled.logger.starter.annotation.Monitor;
import com.fxz.queerer.CacheOperate;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@FilterProperty(filterGroup = Constant.GROUP_QUERY, name = CacheQueryFilter.NAME, order = -10)
public class CacheQueryFilter implements Filter<DefaultDnsQuestion, List<BaseRecord>> {
    @Autowired
    private CacheOperate cacheOperate;

    @Value("${dns.query.filter.CacheQueryFilter.enabled:true}")
    private boolean enabled;

    @Value("${dns.query.cache.fixed.ttl:0}")
    private int fixedTtl;

    @Value("${dns.server.null.value.ttl:60}")
    private Integer nullExpr;
    public static final String NAME = "CacheQueryFilter";

    @Monitor(printParams = false)
    @Trace
    @CatTracing
    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        if (enabled) {
            ActiveSpan.tag("class", CacheQueryFilter.class.getName());
            ActiveSpan.tag("query.name", question.name());
            ActiveSpan.tag("query.type", question.type() + "");
            List<BaseRecord> baseRecords = cacheOperate.get(question.name(), question.type());
            if (CollectionUtils.isEmpty(baseRecords)) {
                if (!cacheOperate.exist(question.name(), question.type().name())) {
                    baseRecords = invoker.invoke(question);
                    if (!CollectionUtils.isEmpty(baseRecords)) {
                        //put into cache
                        cacheOperate.set(question.name(), question.type(), baseRecords, fixedTtl);
                    } else {
                        // null cache
                        cacheOperate.set(question.name(), question.type(), new ArrayList<>(), nullExpr);
                    }
                }
            }
            return baseRecords;
        }
        return invoker.invoke(question);
    }
}
