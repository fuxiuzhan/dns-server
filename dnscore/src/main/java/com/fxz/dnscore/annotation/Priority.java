package com.fxz.dnscore.annotation;

import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/**
 * @author fxz
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface Priority {
    int order() default 0;

    boolean enabled() default true;
}
