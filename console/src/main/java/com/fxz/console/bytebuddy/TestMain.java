package com.fxz.console.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Morph;

import java.io.IOException;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class TestMain {
    public static void main(String[] args) throws IOException, InterruptedException, IllegalAccessException, InstantiationException {
        ByteBuddyAgent.install();
        boolean redefine = false;
        if (redefine) {
            DynamicType.Loaded<TestClass> load = new ByteBuddy()
                    .redefine(TestClass.class)
                    .method(named("say"))
                    .intercept((MethodDelegation.to(TestInterceptor.class)))
                    .make()
                    .load(TestClass.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
            TestClass testClass = load.getLoaded().newInstance();
            System.out.println(testClass.say());
            System.out.println(testClass.say("111111"));
            TestClass tclass = new TestClass();
            System.out.println(tclass.say("22222222"));
        } else {
            DynamicType.Loaded<TestClass> load = new ByteBuddy()
                    .subclass(TestClass.class)
                    .method(named("say"))
                    .intercept(MethodDelegation.withDefaultConfiguration()
                            .withBinders(
                                    // 要用@Morph注解之前，需要通过 Morph.Binder 告诉 Byte Buddy
                                    // 要注入的参数是什么类型
                                    Morph.Binder.install(OverrideCallable.class)
                            )
                            .to(new TestInterceptorWithParams()))
                    .make()
                    .load(TestClass.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
            TestClass testClass = load.getLoaded().newInstance();
            System.out.println(testClass.say());
            System.out.println(testClass.say("111111"));
            TestClass tclass = new TestClass();
            System.out.println(tclass.say("22222222"));
        }
        Thread.sleep(Integer.MAX_VALUE);

    }

}
