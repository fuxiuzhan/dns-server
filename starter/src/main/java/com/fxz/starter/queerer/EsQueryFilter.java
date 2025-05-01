package com.fxz.starter.queerer;

import com.fxz.component.fuled.cat.starter.annotation.CatTracing;
import com.fxz.dnscore.common.Constant;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.exporter.elastic.baserepository.BaseSourceRepository;
import com.fxz.exporter.elastic.objects.SourceRecord;
import com.fxz.fuled.common.chain.Filter;
import com.fxz.fuled.common.chain.Invoker;
import com.fxz.fuled.common.chain.annotation.FilterProperty;
import com.fxz.fuled.logger.starter.annotation.Monitor;
import com.fxz.queerer.util.CacheUtil;
import com.fxz.queerer.util.ConvertUtil;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author fxz
 */
@Slf4j
@FilterProperty(filterGroup = Constant.GROUP_QUERY, name = EsQueryFilter.NAME, order = -30)
public class EsQueryFilter implements Filter<DefaultDnsQuestion, List<BaseRecord>> {
    private BaseSourceRepository sourceRepository;

    public static final String NAME = "CacheQueryFilter";

    public EsQueryFilter(BaseSourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Monitor(printParams = false)
    @Trace
    @CatTracing
    @Override
    public List<BaseRecord> filter(DefaultDnsQuestion question, Invoker<DefaultDnsQuestion, List<BaseRecord>> invoker) {
        log.info("name->{},queryHost->{}", NAME, question.name());
        ActiveSpan.tag("class", EsQueryFilter.class.getName());
        ActiveSpan.tag("query.name", question.name());
        ActiveSpan.tag("query.type", question.type() + "");
        String id = CacheUtil.assembleKey(question.name(), question.type());
        ActiveSpan.tag("query.id", id);
        Optional<SourceRecord> byId = sourceRepository.findById(id);
        if (byId.isPresent()) {
            String result = byId.get().getResult();
            ActiveSpan.tag("query.result", result);
            if (StringUtils.hasText(result)) {
                return ConvertUtil.decodeBaseRecordFromString(result);
            }
        }
        return invoker.invoke(question);
    }
}
