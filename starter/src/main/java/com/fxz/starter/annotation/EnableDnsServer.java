package com.fxz.starter.annotation;

import com.fxz.starter.config.AutoConfigSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fxz
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@Import(EnabledMarker.class)
@Import(AutoConfigSelector.class)
public @interface EnableDnsServer {
}
