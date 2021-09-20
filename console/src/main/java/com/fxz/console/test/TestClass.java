package com.fxz.console.test;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author fuled
 */
public class TestClass {
    public static void main(String[] args) throws InterruptedException {
        RpcContext.get().put("key1", "value1");
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        ThreadPoolExecutor proxy = createProxy(threadPoolExecutor);
        proxy.execute(() -> {
            System.out.println("1111111");
            System.out.println("local->" + RpcContext.get());
        });
        Thread.sleep(100);
        RpcContext.get().put("key1", "value2");
        proxy.execute(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("local1->" + RpcContext.get());
        });
        Thread.sleep(Integer.MAX_VALUE);
    }

    static ThreadPoolExecutor createProxy(ThreadPoolExecutor threadPoolExecutor) {
        return createProxy(threadPoolExecutor
                , Arrays.asList(ThreadPoolExecutor.class.getConstructors()).stream().filter(a -> a.getParameterTypes().length == 7).findFirst().get().getParameterTypes()
                , new Object[]{threadPoolExecutor.getCorePoolSize(), threadPoolExecutor.getMaximumPoolSize()
                        , threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS), TimeUnit.SECONDS
                        , threadPoolExecutor.getQueue(), threadPoolExecutor.getThreadFactory(), threadPoolExecutor.getRejectedExecutionHandler()}
                , new ExecInterceptor());
    }

    static <T> T createProxy(T t, Class[] constructor, Object[] params, MethodInterceptor methodInterceptor) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(t.getClass());
        enhancer.setCallback(methodInterceptor);
        enhancer.setClassLoader(t.getClass().getClassLoader());
        if (constructor == null) {
            return (T) enhancer.create();
        }
        return (T) enhancer.create(constructor, params);
    }

    static class ExecInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            if ("execute".equals(method.getName())) {
                System.out.println("invoked");

                return methodProxy.invokeSuper(o, new Object[]{new ThreadWrapper((Runnable) Arrays.asList(objects).get(0), RpcContext.get())});
            }
            return methodProxy.invokeSuper(o, objects);
        }
    }

    static class ThreadWrapper implements Runnable {
        Runnable runnable;
        Map<Object, Object> objectObjectMap;

        public ThreadWrapper(Runnable runnable, Map map) {
            this.runnable = runnable;
            this.objectObjectMap = new HashMap<>(map);
        }

        @Override
        public void run() {
            //set threadLocal
            RpcContext.set(objectObjectMap);
            System.out.println("thread wrapper....");
            try {
                runnable.run();
            } finally {
                //remove
                RpcContext.remove();
            }
        }
    }

    static class RpcContext {
        static ThreadLocal<Map<Object, Object>> store = ThreadLocal.withInitial(() -> new HashMap<>());

        public static Map get() {
            return store.get();
        }

        public static void set(Map<Object, Object> objectObjectMap) {
            store.set(objectObjectMap);
        }

        public static void remove() {
            store.remove();
        }
    }
}
