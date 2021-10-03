package com.fxz.console.bytebuddy;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;

public class TestInterceptorWithParams {
    @RuntimeType
    public Object intercept(@This Object obj,
                            @AllArguments Object[] allArguments,// 注入目标方法的全部参数
                            @Origin Method method,
                            @Super TestClass testClass,
                            @Morph OverrideCallable callable // 通过@Morph注解注入
    ) throws Throwable {
        try {
            System.out.println("before");
            // 通过 OverrideCallable.call()方法调用目标方法，此时需要传递参数
            Object result = callable.call(allArguments);
            System.out.println("after");
            return result;
        } catch (Throwable t) {
            throw t;
        } finally {
            System.out.println("finally");
        }
    }
}

