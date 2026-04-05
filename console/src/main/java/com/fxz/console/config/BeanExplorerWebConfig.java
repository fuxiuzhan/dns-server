package com.fxz.console.config;

import com.fxz.console.properties.BeanExplorerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(BeanExplorerProperties.class)
public class BeanExplorerWebConfig implements WebMvcConfigurer {

    private final BeanExplorerProperties properties;

    public BeanExplorerWebConfig(BeanExplorerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BeanExplorerAuthInterceptor(properties))
                .addPathPatterns(
                        normalizePath(properties.getPagePath()),
                        "/bean-explorer.html",
                        normalizePath(properties.getApiPath()) + "/**",
                        "/bean-explorer/api/**"
                );
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
