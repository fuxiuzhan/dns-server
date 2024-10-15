package com.fxz.dnscore.queerer;

import com.fxz.dnscore.common.utils.SortUtil;
import com.fxz.dnscore.objects.BaseRecord;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fxz
 */

@Slf4j
public class QueryManger {

    private List<Query> queryList;

    public void setQueryList(List<Query> queryList) {
        this.queryList = SortUtil.sort(queryList);
    }

    @Trace
    public List<BaseRecord> findRecords(DefaultDnsQuestion query) {
        for (Query queerer : queryList) {
            try {
                List<BaseRecord> records = queerer.findRecords(query);
                if (records != null && records.size() > 0) {
                    return records;
                }
            } catch (Exception e) {
                log.error("query error queerer->{},error->", queerer.name(), e);
            }
        }
        return new ArrayList<>();
    }
}
