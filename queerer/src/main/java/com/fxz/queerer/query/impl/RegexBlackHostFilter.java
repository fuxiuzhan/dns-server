package com.fxz.queerer.query.impl;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.fuled.common.chain.Filter;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.common.chain.annotation.FilterProperty;
import com.fxz.fuled.logger.starter.annotation.Monitor;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@FilterProperty(filterGroup = Constant.GROUP_QUERY, name = RegexBlackHostFilter.NAME, order = 30)
public class RegexBlackHostFilter implements Filter<DefaultDnsQuestion, List<BaseRecord>> {

    @Value("${dns.query.filter.RegexBlackHostFilter.enabled:true}")
    private boolean enabled;
    @Value("${dns.server.black.regex:}")
    private List<String> regexList;
    public static final String NAME = "RegexBlackHostFilter";

    @Monitor(printParams = false)
    @Trace
    @CatTracing
    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        if (enabled) {
            if (!CollectionUtils.isEmpty(regexList)) {
                for (String s : regexList) {
                    if (question.name().contains(s)) {
                        return new ArrayList<>();
                    }
                }
            }
            return invoker.invoke(question);
        }
        return invoker.invoke(question);
    }
}
