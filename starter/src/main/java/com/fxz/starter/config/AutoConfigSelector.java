package com.fxz.starter.config;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author fxz
 */
public class AutoConfigSelector implements ImportSelector {
    private String CLASSNAME = "com.fxz.starter.config.EnabledMarker";

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{CLASSNAME};
    }
}
