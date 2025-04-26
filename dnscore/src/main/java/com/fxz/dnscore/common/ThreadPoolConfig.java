package com.fxz.dnscore.common;

import com.fxz.fuled.common.utils.ThreadFactoryNamed;
import com.fxz.fuled.dynamic.threadpool.ThreadPoolRegistry;
import com.fxz.fuled.dynamic.threadpool.manage.impl.DefaultThreadExecuteHook;

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
    static ThreadPoolExecutor querySyncThreadPool = null;

    static {
        singleThreadPoolInstance = getThreadPool("dns-export-thread-pool");
        querySyncThreadPool = getThreadPool("dns-sync-thread-pool");
    }

    public static ThreadPoolExecutor getThreadPoolInstance() {
        return singleThreadPoolInstance;
    }

    public static ThreadPoolExecutor getQueryThreadPool() {
        return querySyncThreadPool;
    }

    private static ThreadPoolExecutor getThreadPool(String poolName) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_THREADS, CORE_THREADS * 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), ThreadFactoryNamed.named(THREAD_POOL_PREFIX));
        executor.allowCoreThreadTimeOut(true);
        ThreadPoolRegistry.registerThreadPool(poolName, executor, new TracedThreadExecuteHook());
        return executor;
    }

}
