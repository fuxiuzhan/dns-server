package com.fxz.dnscore.common;

import com.fxz.fuled.common.utils.ThreadFactoryNamed;
import com.fxz.fuled.dynamic.threadpool.ThreadPoolRegistry;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author fxz
 */
public class ThreadPoolConfig implements SmartInitializingSingleton {
    static final int CORE_THREADS = Math.max(16, Runtime.getRuntime().availableProcessors() * 4);
    static final String THREAD_POOL_PREFIX = "common-";
    static ThreadPoolExecutor exportThreadPool = null;
    static ThreadPoolExecutor querySyncThreadPool = null;

    public static final String EXPORT_THREAD_POOL = "dns-export-thread-pool";
    public static final String QUERY_THREAD_POOL = "dns-sync-thread-pool";

    @Autowired
    private TracedThreadExecuteHook tracedThreadExecuteHook;

    static {
        exportThreadPool = getThreadPool(EXPORT_THREAD_POOL);
        querySyncThreadPool = getThreadPool(QUERY_THREAD_POOL);
    }

    public static ThreadPoolExecutor getExportThreadPool() {
        return exportThreadPool;
    }

    public static ThreadPoolExecutor getQueryThreadPool() {
        return querySyncThreadPool;
    }

    private static ThreadPoolExecutor getThreadPool(String poolName) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_THREADS, CORE_THREADS * 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), ThreadFactoryNamed.named(THREAD_POOL_PREFIX));
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    @Override
    public void afterSingletonsInstantiated() {
        ThreadPoolRegistry.registerThreadPool(EXPORT_THREAD_POOL, exportThreadPool, tracedThreadExecuteHook);
        ThreadPoolRegistry.registerThreadPool(QUERY_THREAD_POOL, querySyncThreadPool, tracedThreadExecuteHook);
    }
}
