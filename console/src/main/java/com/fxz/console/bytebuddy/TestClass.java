package com.fxz.console.bytebuddy;

public class TestClass {

    public String say() {
        return "native say";
    }

    public String say(String raw) {
        return "native " + raw;
    }
}
