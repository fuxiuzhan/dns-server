package com.fxz.console.controller;

import com.fxz.console.properties.BeanExplorerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@RestController
@ConditionalOnProperty(prefix = "bean.explorer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BeanExplorerPageController {

    private final BeanExplorerProperties properties;

    public BeanExplorerPageController(BeanExplorerProperties properties) {
        this.properties = properties;
    }

    @GetMapping(value = "${bean.explorer.page-path:/bean-explorer.html}", produces = MediaType.TEXT_HTML_VALUE)
    public String page() throws Exception {
        ClassPathResource resource = new ClassPathResource("static/bean-explorer.html");
        String html = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        return html
                .replace("__BEAN_EXPLORER_API_BASE__", normalizePath(properties.getApiPath()))
                .replace("__BEAN_EXPLORER_AUTH_ENABLED__", String.valueOf(properties.getAuth().isEnabled()))
                .replace("__BEAN_EXPLORER_AUTH_HEADER__", escapeJs(properties.getAuth().getHeaderName()))
                .replace("__BEAN_EXPLORER_AUTH_QUERY__", escapeJs(properties.getAuth().getQueryParam()));
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
