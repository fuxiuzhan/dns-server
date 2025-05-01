package com.fxz.queerer.query.impl;

import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.fuled.common.chain.Filter;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.common.chain.annotation.FilterProperty;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@FilterProperty(filterGroup = Constant.GROUP_QUERY, name = BeforeQueryFilter.NAME)
@Slf4j
public class BeforeQueryFilter implements Filter<DefaultDnsQuestion, List<BaseRecord>> {
    public static final String NAME = "BeforeQueryFilter";

    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        log.info("name->{},queryHost->{}", NAME, question.name());
        return invoker.invoke(question);
    }
}
