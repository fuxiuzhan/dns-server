package com.fxz.console.config;

import com.fxz.console.properties.BeanExplorerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BeanExplorerAuthInterceptor implements HandlerInterceptor {

    private final BeanExplorerProperties properties;

    public BeanExplorerAuthInterceptor(BeanExplorerProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        String configuredPagePath = normalizePath(properties.getPagePath());
        if ("/bean-explorer.html".equals(requestUri) && !"/bean-explorer.html".equals(configuredPagePath)) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return false;
        }
        if (!properties.isEnabled()) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return false;
        }
        if (configuredPagePath.equals(requestUri)) {
            return true;
        }
        if (!properties.getAuth().isEnabled()) {
            return true;
        }
        String expected = properties.getAuth().getToken();
        if (!StringUtils.hasText(expected)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Bean Explorer token is not configured");
            return false;
        }
        String actual = request.getHeader(properties.getAuth().getHeaderName());
        if (!StringUtils.hasText(actual)) {
            actual = request.getParameter(properties.getAuth().getQueryParam());
        }
        if (!expected.equals(actual)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid Bean Explorer token");
            return false;
        }
        return true;
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
