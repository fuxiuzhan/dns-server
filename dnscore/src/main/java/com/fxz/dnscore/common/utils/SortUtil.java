package com.fxz.dnscore.common.utils;

import com.fxz.dnscore.annotation.Priority;
import com.fxz.dnscore.processor.Processor;
import io.netty.handler.codec.dns.DnsRecordType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fxz
 */
public class SortUtil {

    /**
     * sort anno
     *
     * @param list
     * @param <T>
     * @return
     */
    public static <T> List<T> sort(List<T> list) {
        if (list == null || list.size() == 0) {
            return list;
        }
        list.sort((o1, o2) -> {
            if (o1.equals(o2)) {
                return 0;
            }
            int order1 = 0;
            int order2 = 0;
            Priority annotation1 = o1.getClass().getAnnotation(Priority.class);
            if (annotation1 != null) {
                order1 = annotation1.order();
            }
            Priority annotation2 = o2.getClass().getAnnotation(Priority.class);
            if (annotation2 != null) {
                order2 = annotation2.order();
            }
            return order1 - order2;
        });
        return list;
    }

    public static Map<DnsRecordType, Processor> getProcessorMap(List<Processor> processorList) {
        if (processorList == null || processorList.size() == 0) {
            return null;
        }
        Map<DnsRecordType, Processor> processorMap = new HashMap<>();
        for (Processor processor : processorList) {
            processorMap.put(processor.type(), processor);
        }
        return processorMap;
    }
}
