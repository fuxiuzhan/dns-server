package com.fxz.console.service;

import com.fxz.console.pojo.beanexplorer.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class BeanExplorerService {

    private static final int MAX_CHILDREN = 200;

    private final ApplicationContext applicationContext;

    public BeanExplorerService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<BeanTypeGroup> listBeans() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Map<String, List<BeanSummary>> grouped = new TreeMap<String, List<BeanSummary>>();
        for (String beanName : beanNames) {
            try {
                Object bean = unwrapProxy(applicationContext.getBean(beanName));
                Class<?> beanClass = resolveDisplayClass(bean);
                BeanSummary summary = new BeanSummary();
                summary.setBeanName(beanName);
                summary.setClassName(beanClass.getName());
                String typeName = beanClass.getName();
                if (!grouped.containsKey(typeName)) {
                    grouped.put(typeName, new ArrayList<BeanSummary>());
                }
                grouped.get(typeName).add(summary);
            } catch (Throwable ignored) {
                // Ignore beans that fail to load
            }
        }
        List<BeanTypeGroup> result = new ArrayList<BeanTypeGroup>();
        for (Map.Entry<String, List<BeanSummary>> entry : grouped.entrySet()) {
            entry.getValue().sort(Comparator.comparing(BeanSummary::getBeanName));
            BeanTypeGroup group = new BeanTypeGroup();
            group.setTypeName(entry.getKey());
            group.setCount(entry.getValue().size());
            group.setBeans(entry.getValue());
            result.add(group);
        }
        return result;
    }

    public BeanInspectResponse inspect(BeanInspectRequest request) {
        if (request == null || !StringUtils.hasText(request.getBeanName())) {
            throw new IllegalArgumentException("beanName 不能为空");
        }
        Object rootBean;
        try {
            rootBean = unwrapProxy(applicationContext.getBean(request.getBeanName()));
        } catch (Exception e) {
            throw new IllegalArgumentException("无法获取 Bean: " + request.getBeanName(), e);
        }
        Object current = navigate(rootBean, request.getPath());

        BeanInspectResponse response = new BeanInspectResponse();
        response.setBeanName(request.getBeanName());
        response.setRootClassName(classNameOf(resolveDisplayClass(rootBean)));
        response.setCurrentClassName(classNameOf(current));
        response.setValuePreview(renderValue(current));
        response.setNodeKind(detectNodeKind(current));
        response.setPath(copyPath(request.getPath()));
        response.setChildren(buildChildren(request.getPath(), current));
        return response;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public Object getBean(String beanName) {
        return unwrapProxy(applicationContext.getBean(beanName));
    }

    public String describeValue(Object value) {
        return renderValue(value);
    }

    public Object navigate(Object root, List<PathStep> path) {
        Object current = root;
        if (CollectionUtils.isEmpty(path)) {
            return unwrapProxy(current);
        }
        for (PathStep step : path) {
            current = access(current, step);
        }
        return unwrapProxy(current);
    }

    private Object access(Object current, PathStep step) {
        if (current == null) {
            return null;
        }
        if (step == null || !StringUtils.hasText(step.getKind())) {
            throw new IllegalArgumentException("路径节点非法");
        }
        if ("FIELD".equalsIgnoreCase(step.getKind())) {
            Field field = findField(current.getClass(), step.getName());
            if (field == null) {
                throw new IllegalArgumentException("字段不存在: " + step.getName());
            }
            try {
                field.setAccessible(true);
                return unwrapProxy(field.get(current));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("字段访问失败: " + step.getName(), e);
            }
        }
        if ("INDEX".equalsIgnoreCase(step.getKind())) {
            Integer index = step.getIndex();
            if (index == null || index < 0) {
                throw new IllegalArgumentException("索引非法");
            }
            if (current.getClass().isArray()) {
                if (index >= Array.getLength(current)) {
                    throw new IllegalArgumentException("索引越界: " + index);
                }
                return unwrapProxy(Array.get(current, index));
            }
            if (current instanceof List) {
                List<?> list = (List<?>) current;
                if (index >= list.size()) {
                    throw new IllegalArgumentException("索引越界: " + index);
                }
                return unwrapProxy(list.get(index));
            }
            throw new IllegalArgumentException("当前节点不支持索引访问");
        }
        if ("MAP_KEY".equalsIgnoreCase(step.getKind())) {
            if (!(current instanceof Map)) {
                throw new IllegalArgumentException("当前节点不是 Map");
            }
            return unwrapProxy(((Map<?, ?>) current).get(step.getMapKey()));
        }
        throw new IllegalArgumentException("未知路径类型: " + step.getKind());
    }

    public List<BeanPropertyNode> buildChildren(List<PathStep> basePath, Object current) {
        if (!isExpandable(current)) {
            return Collections.emptyList();
        }
        if (current instanceof Map) {
            return buildMapChildren(basePath, (Map<?, ?>) current);
        }
        if (current instanceof List) {
            return buildListChildren(basePath, ((List<?>) current));
        }
        if (current.getClass().isArray()) {
            return buildArrayChildren(basePath, current);
        }
        return buildFieldChildren(basePath, current);
    }

    private List<BeanPropertyNode> buildFieldChildren(List<PathStep> basePath, Object current) {
        List<BeanPropertyNode> children = new ArrayList<BeanPropertyNode>();
        for (Field field : collectFields(current.getClass())) {
            BeanPropertyNode node = new BeanPropertyNode();
            node.setLabel(field.getName());
            node.setPath(appendStep(basePath, fieldStep(field.getName())));
            try {
                field.setAccessible(true);
                Object value = unwrapProxy(field.get(current));
                node.setClassName(classNameOf(value == null ? field.getType() : value));
                node.setValuePreview(renderValue(value));
                node.setNodeKind(detectNodeKind(value == null ? field.getType() : value));
                node.setExpandable(isExpandable(value));
            } catch (Exception e) {
                node.setClassName(field.getType().getName());
                node.setValuePreview("读取失败");
                node.setNodeKind("error");
                node.setError(e.getMessage());
                node.setExpandable(false);
            }
            children.add(node);
            if (children.size() >= MAX_CHILDREN) {
                break;
            }
        }
        return children;
    }

    private List<BeanPropertyNode> buildListChildren(List<PathStep> basePath, List<?> list) {
        List<BeanPropertyNode> children = new ArrayList<BeanPropertyNode>();
        for (int i = 0; i < list.size() && i < MAX_CHILDREN; i++) {
            Object value = unwrapProxy(list.get(i));
            BeanPropertyNode node = buildValueNode("[" + i + "]", value, appendStep(basePath, indexStep(i)));
            children.add(node);
        }
        return children;
    }

    private List<BeanPropertyNode> buildArrayChildren(List<PathStep> basePath, Object array) {
        List<BeanPropertyNode> children = new ArrayList<BeanPropertyNode>();
        int length = Array.getLength(array);
        for (int i = 0; i < length && i < MAX_CHILDREN; i++) {
            Object value = unwrapProxy(Array.get(array, i));
            BeanPropertyNode node = buildValueNode("[" + i + "]", value, appendStep(basePath, indexStep(i)));
            children.add(node);
        }
        return children;
    }

    private List<BeanPropertyNode> buildMapChildren(List<PathStep> basePath, Map<?, ?> map) {
        List<BeanPropertyNode> children = new ArrayList<BeanPropertyNode>();
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= MAX_CHILDREN) {
                break;
            }
            String key = String.valueOf(entry.getKey());
            Object value = unwrapProxy(entry.getValue());
            BeanPropertyNode node = buildValueNode("{" + key + "}", value, appendStep(basePath, mapKeyStep(key)));
            children.add(node);
            count++;
        }
        return children;
    }

    public BeanPropertyNode buildValueNode(String label, Object value, List<PathStep> path) {
        BeanPropertyNode node = new BeanPropertyNode();
        node.setLabel(label);
        node.setPath(path);
        node.setClassName(classNameOf(value));
        node.setValuePreview(renderValue(value));
        node.setNodeKind(detectNodeKind(value));
        node.setExpandable(isExpandable(value));
        return node;
    }

    private List<Field> collectFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        Set<String> names = new HashSet<String>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isSynthetic()) {
                    continue;
                }
                if (names.add(field.getName())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        fields.sort(Comparator.comparing(Field::getName));
        return fields;
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private boolean isExpandable(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Class) {
            return false;
        }
        Class<?> type = value.getClass();
        if (isSimpleType(type)) {
            return false;
        }
        if (type.isArray()) {
            return Array.getLength(value) > 0;
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        return !collectFields(type).isEmpty();
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type)
                || UUID.class.isAssignableFrom(type)
                || type.getName().startsWith("java.time.")
                || type.getName().startsWith("java.lang.");
    }

    private String renderValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value);
        }
        if (value.getClass().isArray()) {
            return "Array(length=" + Array.getLength(value) + ")";
        }
        if (value instanceof Collection) {
            return value.getClass().getSimpleName() + "(size=" + ((Collection<?>) value).size() + ")";
        }
        if (value instanceof Map) {
            return value.getClass().getSimpleName() + "(size=" + ((Map<?, ?>) value).size() + ")";
        }
        if (isSimpleType(value.getClass())) {
            return String.valueOf(value);
        }
        return classNameOf(value) + "@" + Integer.toHexString(System.identityHashCode(value));
    }

    private String detectNodeKind(Object value) {
        if (value == null) {
            return "null";
        }
        Class<?> type = value instanceof Class ? (Class<?>) value : value.getClass();
        if (type.isArray()) {
            return "array";
        }
        if (Collection.class.isAssignableFrom(type)) {
            return "collection";
        }
        if (Map.class.isAssignableFrom(type)) {
            return "map";
        }
        if (isSimpleType(type)) {
            return "value";
        }
        return "object";
    }

    private String classNameOf(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Class) {
            return ((Class<?>) value).getName();
        }
        return value.getClass().getName();
    }

    private Class<?> resolveDisplayClass(Object bean) {
        bean = unwrapProxy(bean);
        if (bean == null) {
            return Object.class;
        }
        Class<?> beanClass = bean.getClass();
        if (Proxy.isProxyClass(beanClass)) {
            Class<?>[] interfaces = beanClass.getInterfaces();
            if (interfaces.length > 0) {
                return interfaces[0];
            }
        }
        if (beanClass.getName().contains("$$") && beanClass.getSuperclass() != null && beanClass.getSuperclass() != Object.class) {
            return beanClass.getSuperclass();
        }
        return beanClass;
    }

    private Object unwrapProxy(Object candidate) {
        Object current = candidate;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        while (current != null && visited.add(current)) {
            Object next = extractSpringProxyTarget(current);
            if (next == null || next == current) {
                return current;
            }
            current = next;
        }
        return current;
    }

    private Object extractSpringProxyTarget(Object candidate) {
        Object jdkTarget = extractJdkDynamicProxyTarget(candidate);
        if (jdkTarget != null && jdkTarget != candidate) {
            return jdkTarget;
        }
        Object cglibTarget = extractCglibProxyTarget(candidate);
        if (cglibTarget != null && cglibTarget != candidate) {
            return cglibTarget;
        }
        return candidate;
    }

    private Object extractJdkDynamicProxyTarget(Object candidate) {
        if (candidate == null || !Proxy.isProxyClass(candidate.getClass())) {
            return null;
        }
        try {
            Object handler = java.lang.reflect.Proxy.getInvocationHandler(candidate);
            return extractTargetFromAdvisedSupport(handler);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object extractCglibProxyTarget(Object candidate) {
        if (candidate == null) {
            return null;
        }
        Class<?> current = candidate.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.getName().startsWith("CGLIB$CALLBACK_")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object callback = field.get(candidate);
                    Object target = extractTargetFromAdvisedSupport(callback);
                    if (target != null) {
                        return target;
                    }
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Object extractTargetFromAdvisedSupport(Object source) {
        if (source == null) {
            return null;
        }
        Object advised = findFieldValue(source, "advised");
        if (advised == null) {
            advised = findFieldValue(source, "advisedSupport");
        }
        if (advised == null) {
            return null;
        }
        try {
            Method method = advised.getClass().getMethod("getTargetSource");
            Object targetSource = method.invoke(advised);
            if (targetSource == null) {
                return null;
            }
            Method getTarget = targetSource.getClass().getMethod("getTarget");
            return getTarget.invoke(targetSource);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object findFieldValue(Object source, String fieldName) {
        Field field = findField(source.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(source);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private List<PathStep> copyPath(List<PathStep> path) {
        if (CollectionUtils.isEmpty(path)) {
            return Collections.emptyList();
        }
        List<PathStep> copy = new ArrayList<PathStep>();
        for (PathStep step : path) {
            PathStep item = new PathStep();
            item.setKind(step.getKind());
            item.setName(step.getName());
            item.setIndex(step.getIndex());
            item.setMapKey(step.getMapKey());
            copy.add(item);
        }
        return copy;
    }

    private List<PathStep> appendStep(List<PathStep> basePath, PathStep step) {
        List<PathStep> result = new ArrayList<PathStep>();
        if (!CollectionUtils.isEmpty(basePath)) {
            result.addAll(copyPath(basePath));
        }
        result.add(step);
        return result;
    }

    private PathStep fieldStep(String fieldName) {
        PathStep step = new PathStep();
        step.setKind("FIELD");
        step.setName(fieldName);
        return step;
    }

    private PathStep indexStep(int index) {
        PathStep step = new PathStep();
        step.setKind("INDEX");
        step.setIndex(index);
        return step;
    }

    private PathStep mapKeyStep(String key) {
        PathStep step = new PathStep();
        step.setKind("MAP_KEY");
        step.setMapKey(key);
        return step;
    }

}
