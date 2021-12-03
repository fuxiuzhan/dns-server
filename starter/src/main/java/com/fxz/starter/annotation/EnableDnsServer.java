package com.fxz.starter.annotation;


import com.fxz.starter.config.AutoConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fuled
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AutoConfig.class)
public @interface EnableDnsServer {
}
