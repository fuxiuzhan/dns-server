package com.fxz.dnscore.common;

import com.fxz.fuled.common.utils.ThreadFactoryNamed;
import com.fxz.fuled.dynamic.threadpool.ThreadPoolRegistry;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author fxz
 */
public class ThreadPoolConfig {
    static final int CORE_THREADS = Math.max(16, Runtime.getRuntime().availableProcessors() * 4);
    static final String THREAD_POOL_PREFIX = "common-";
    static ThreadPoolExecutor singleThreadPoolInstance = null;

    static {
        singleThreadPoolInstance = getThreadPool();
    }

    public static ThreadPoolExecutor getThreadPoolInstance() {
        return singleThreadPoolInstance;
    }

    private static ThreadPoolExecutor getThreadPool() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_THREADS, CORE_THREADS * 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), ThreadFactoryNamed.named(THREAD_POOL_PREFIX));
        executor.allowCoreThreadTimeOut(true);
        ThreadPoolRegistry.registerThreadPool("dns-export-thread-pool", executor);
        return executor;
    }

}
