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

import java.util.List;

@FilterProperty(filterGroup = Constant.GROUP_QUERY, name = BeforeQueryFilter.NAME)
@Slf4j
public class BeforeQueryFilter implements Filter<DefaultDnsQuestion, List<BaseRecord>> {
    public static final String NAME = "BeforeQueryFilter";

    @Monitor(printParams = false)
    @Trace
    @CatTracing
    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        log.info("name->{},queryHost->{}", NAME, question.name());
        return invoker.invoke(question);
    }
}
