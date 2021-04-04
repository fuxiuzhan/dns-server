package com.fxz.dnscore.objects.common;

import com.fxz.dnscore.io.DatagramDnsResponse;
import lombok.Data;

import java.util.concurrent.CountDownLatch;

/**
 * @author xiuzhan.fu
 */
@Data
public class ResponseSemaphore {
    private CountDownLatch countDownLatch;
    private DatagramDnsResponse response;
}
