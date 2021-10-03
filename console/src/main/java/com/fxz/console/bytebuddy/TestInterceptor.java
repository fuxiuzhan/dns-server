package com.fxz.console.bytebuddy;

public class TestInterceptor {

    public static String intercept() {
        return "invokerd";
    }

    public static String intercept(String raw) {
        return "invoked" + raw;
    }
}
