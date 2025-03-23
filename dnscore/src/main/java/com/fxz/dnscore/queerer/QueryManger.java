package com.fxz.dnscore.queerer;

import com.fxz.dnscore.common.utils.SortUtil;
import com.fxz.dnscore.objects.BaseRecord;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author fxz
 */

@Slf4j
public class QueryManger {

    @Value("#{${dns.server.filter.ips:{}}}")
    private Map<String, List<String>> filterIps;

    private List<Query> queryList;

    public void setQueryList(List<Query> queryList) {
        this.queryList = SortUtil.sort(queryList);
    }

    @Trace
    public List<BaseRecord> findRecords(DefaultDnsQuestion question, DatagramDnsQuery query) {
        for (Query queerer : queryList) {
            try {
                List<BaseRecord> records = queerer.findRecords(question);
                if (records != null && records.size() > 0) {
                    List<String> orDefault = filterIps.getOrDefault(query.sender().getAddress().getHostAddress(), new ArrayList<>());
                    if (orDefault.contains(query.recordAt(DnsSection.QUESTION).type().name())) {
                        return new ArrayList<>();
                    }
                    return records;
                }
            } catch (Exception e) {
                log.error("query error queerer->{},error->", queerer.name(), e);
            }
        }
        return new ArrayList<>();
    }
}
