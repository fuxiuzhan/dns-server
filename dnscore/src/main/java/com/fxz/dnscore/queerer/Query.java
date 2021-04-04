package com.fxz.dnscore.queerer;

import com.fxz.dnscore.objects.BaseRecord;
import io.netty.handler.codec.dns.DefaultDnsQuestion;

import java.util.ArrayList;
import java.util.List;

public interface Query {
    default String name() {
        return "defaultQuery";
    }

    /**
     * @param question
     * @return
     */
    default List<BaseRecord> findRecords(DefaultDnsQuestion question) {
        return new ArrayList<>();
    }
}
