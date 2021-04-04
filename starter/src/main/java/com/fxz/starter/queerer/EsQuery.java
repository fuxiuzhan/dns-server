package com.fxz.starter.queerer;

import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.dnscore.queerer.Query;
import com.fxz.exporter.elastic.objects.SourceRecord;
import com.fxz.queerer.util.CacheUtil;
import com.fxz.queerer.util.ConvertUtil;
import com.fxz.starter.repository.SourceRepository;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author fxz
 */
@Slf4j
@Priority(order = 3)
public class EsQuery implements Query {
    SourceRepository sourceRepository;

    @Override
    public String name() {
        return "esQuery";
    }

    public EsQuery(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Override
    public List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        log.info("name->{},queryHost->{}", name(), question.name());
        if (question != null) {
            String id = CacheUtil.assembleKey(question.name(), question.type());
            Optional<SourceRecord> byId = sourceRepository.findById(id);
            if (byId.isPresent()) {
                String result = byId.get().getResult();
                if (StringUtils.hasText(result)) {
                    return ConvertUtil.decodeBaseRecordFromString(result);
                }
            }
        }
        /**
         * check ptr return
         */
        return null;
    }
}
