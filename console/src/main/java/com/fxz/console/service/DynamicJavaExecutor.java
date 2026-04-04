package com.fxz.console.service;

import com.fxz.console.pojo.beanexplorer.ExecuteCodeResponse;
import com.fxz.console.pojo.beanexplorer.BeanInspectResponse;
import com.fxz.console.pojo.beanexplorer.PathStep;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Service
public class DynamicJavaExecutor {

    private final BeanExplorerService beanExplorerService;

    private Object lastResult;

    public DynamicJavaExecutor(BeanExplorerService beanExplorerService) {
        this.beanExplorerService = beanExplorerService;
    }

    public ExecuteCodeResponse execute(String javaCode) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        if (!StringUtils.hasText(javaCode)) {
            response.setSuccess(false);
            response.setError("javaCode 不能为空");
            return response;
        }

        try {
            ApplicationContext applicationContext = beanExplorerService.getApplicationContext();
            Binding binding = new Binding();

            // Inject all beans as variables
            String[] beanNames = applicationContext.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                try {
                    if (isValidVariableName(beanName)) {
                        binding.setVariable(beanName, applicationContext.getBean(beanName));
                    }
                } catch (Exception ignored) {
                }
            }

            // Also inject ctx and applicationContext
            ExecutionContext ctx = new ExecutionContext(applicationContext, beanExplorerService);
            binding.setVariable("ctx", ctx);
            binding.setVariable("applicationContext", applicationContext);

            // Configure common imports
            ImportCustomizer importCustomizer = new ImportCustomizer();
            importCustomizer.addStarImports("java.util", "java.util.concurrent", "java.lang.reflect");
            CompilerConfiguration config = new CompilerConfiguration();
            config.addCompilationCustomizers(importCustomizer);

            GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding, config);
            Object result = shell.evaluate(javaCode);

            response.setSuccess(true);
            response.setResultType(result == null ? "null" : result.getClass().getName());
            response.setResult(beanExplorerService.describeValue(result));
            response.setResultNode(beanExplorerService.buildValueNode("result", result, Collections.emptyList()));
            this.lastResult = result;
            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
            return response;
        }
    }

    private boolean isValidVariableName(String name) {
        if (!StringUtils.hasText(name)) return false;
        if (!Character.isJavaIdentifierStart(name.charAt(0))) return false;
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
        }
        return true;
    }

    public BeanInspectResponse inspectResult(List<PathStep> path) {
        BeanInspectResponse response = new BeanInspectResponse();
        if (lastResult == null) {
            response.setChildren(Collections.emptyList());
            return response;
        }
        try {
            Object target = beanExplorerService.navigate(lastResult, path);
            response.setChildren(beanExplorerService.buildChildren(path, target));
        } catch (Exception e) {
            // error handling
        }
        return response;
    }

    public static class ExecutionContext {

        private final ApplicationContext applicationContext;

        private final BeanExplorerService beanExplorerService;

        public ExecutionContext(ApplicationContext applicationContext, BeanExplorerService beanExplorerService) {
            this.applicationContext = applicationContext;
            this.beanExplorerService = beanExplorerService;
        }

        public ApplicationContext applicationContext() {
            return applicationContext;
        }

        public Object bean(String beanName) {
            return beanExplorerService.getBean(beanName);
        }

        public <T> T bean(String beanName, Class<T> type) {
            return type.cast(bean(beanName));
        }

        public Object field(Object target, String fieldName) throws Exception {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        }

        public void setField(Object target, String fieldName, Object value) throws Exception {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }

        public Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
            Method method = findMethod(target.getClass(), methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        }

        private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                try {
                    return current.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
                current = current.getSuperclass();
            }
            throw new NoSuchFieldException(fieldName);
        }

        private Method findMethod(Class<?> type, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                try {
                    return current.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                }
                current = current.getSuperclass();
            }
            throw new NoSuchMethodException(methodName);
        }
    }
}
