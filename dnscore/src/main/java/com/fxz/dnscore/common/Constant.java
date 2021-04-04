package com.fxz.dnscore.common;

import com.fxz.dnscore.objects.common.ResponseSemaphore;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiuzhan.fu
 */
@Data
public class Constant {
    public static Map<Integer, ResponseSemaphore> singleMap = new ConcurrentHashMap<>();
    private static final boolean CONNECTING = true;
    public static volatile boolean netStat = CONNECTING;
}
