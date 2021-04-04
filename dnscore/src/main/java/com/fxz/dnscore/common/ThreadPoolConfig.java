package com.fxz.dnscore.common;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class ThreadPoolConfig {
    static AtomicLong counter = new AtomicLong(0);
    static final int CORE_THREADS = Math.max(16, Runtime.getRuntime().availableProcessors() * 4);
    static final String THREAD_POOL_PREFIX = "common-";

    public static ThreadPoolExecutor getThreadPool() {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_THREADS, CORE_THREADS * 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName(THREAD_POOL_PREFIX + counter.getAndIncrement());
                return thread;
            }
        });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

}
