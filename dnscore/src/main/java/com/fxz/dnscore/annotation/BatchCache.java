package com.fxz.dnscore.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/**
 * @author fxz
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface BatchCache {
    Cache[] value() default {};

    @AliasFor("value")
    Cache[] caches() default {};
}
