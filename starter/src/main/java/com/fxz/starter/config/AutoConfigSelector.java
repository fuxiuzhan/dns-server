package com.fxz.starter.config;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author fxz
 */
public class AutoConfigSelector implements ImportSelector {
    private String CLASSNAME = "com.fxz.starter.config.EnabledMarker";
    private String AUTOCLASS="com.fxz.starter.config.AutoConfig";

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{CLASSNAME,AUTOCLASS};
    }
}
