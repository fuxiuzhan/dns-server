package com.fxz.queerer.query.impl;

import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.fuled.common.chain.Filter;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.common.chain.annotation.FilterProperty;
import com.fxz.queerer.CacheOperate;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
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

    @Value("${dns.server.null.value.ttl:60}")
    private Integer nullExpr;
    public static final String NAME = "CacheQueryFilter";

    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        ActiveSpan.tag("class", CacheQueryFilter.class.getName());
        ActiveSpan.tag("query.name", question.name());
        ActiveSpan.tag("query.type", question.type() + "");
        List<BaseRecord> baseRecords = cacheOperate.get(question.name(), question.type());
        if (CollectionUtils.isEmpty(baseRecords) && !cacheOperate.exist(question.name(), question.type().name())) {
            List<BaseRecord> invoke = invoker.invoke(question);
            if (CollectionUtils.isEmpty(invoke)) {
                //nop cache
                cacheOperate.set(question.name(), question.type(), new ArrayList<>(), nullExpr);
            }
            return invoke;
        }
        return baseRecords;
    }
}
