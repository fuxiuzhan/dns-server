package com.fxz.console.service;

import com.fxz.console.pojo.beanexplorer.BeanInspectRequest;
import com.fxz.console.pojo.beanexplorer.BeanInspectResponse;
import com.fxz.console.pojo.beanexplorer.BeanPropertyNode;
import com.fxz.console.pojo.beanexplorer.ExecuteCodeResponse;
import com.fxz.console.pojo.beanexplorer.MemberHint;
import com.fxz.console.pojo.beanexplorer.MembersResponse;
import com.fxz.console.pojo.beanexplorer.PathStep;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DynamicJavaExecutor {

    private static final Pattern SIMPLE_BEAN_PROPERTY_PATH = Pattern.compile("^([a-zA-Z_$][\\w$]*)(\\.[a-zA-Z_$][\\w$]*)*$");

    private final BeanExplorerService beanExplorerService;

    private final JavaLikeExpressionExecutor expressionExecutor = new JavaLikeExpressionExecutor();

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
            String normalized = normalizeExecutableCode(javaCode);
            if (!StringUtils.hasText(normalized)) {
                response.setSuccess(false);
                response.setError("没有可执行的表达式（可能只有注释或空行）");
                return response;
            }
            Map<String, Object> roots = buildRoots();
            Object result = expressionExecutor.evaluate(normalized, roots);
            Object stored = beanExplorerService.unwrapValue(result);
            this.lastResult = stored;

            response.setSuccess(true);
            response.setResultType(stored == null ? "null" : stored.getClass().getName());
            response.setResult(beanExplorerService.describeValue(stored));
            response.setResultNode(beanExplorerService.buildValueNode("result", stored, Collections.<PathStep>emptyList()));
            return response;
        } catch (JavaLikeExpressionExecutor.ExpressionException e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
            return response;
        }
    }

    /**
     * 多行时合并为多条语句（以 ; 分隔），求值结果为最后一条的值；跳过纯注释行与空行。
     * 解决首行 // 说明、第二行 bean.prop 时原先只执行最后一行却未合并的问题。
     */
    static String normalizeExecutableCode(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\r\n|\n|\r")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("//")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(t);
        }
        return sb.toString().trim();
    }

    /**
     * 自动补全：优先走与左侧 Bean 树相同的 {@link BeanExplorerService#inspect}，得到同一套子节点；
     * 再附加反射方法（如 List.add），便于类型自带操作。
     */
    public MembersResponse members(String expression) {
        MembersResponse r = new MembersResponse();
        if (!StringUtils.hasText(expression)) {
            r.setValueTypeName("");
            r.setHints(Collections.<MemberHint>emptyList());
            return r;
        }
        String trimmed = expression.trim();
        if (isSimpleBeanPropertyPath(trimmed)) {
            try {
                BeanInspectRequest req = buildInspectRequestForReceiver(trimmed);
                BeanInspectResponse insp = beanExplorerService.inspect(req);
                r.setValueTypeName(insp.getCurrentClassName());
                List<MemberHint> hints = hintsFromInspectChildren(insp.getChildren());
                Object root = beanExplorerService.getBean(req.getBeanName());
                Object current = beanExplorerService.navigate(root, req.getPath());
                Set<String> propertyFieldNames = new HashSet<String>();
                for (MemberHint h : hints) {
                    if ("PROPERTY".equals(h.getKind()) && isSimpleFieldName(h.getName())) {
                        propertyFieldNames.add(h.getName());
                    }
                }
                for (MemberHint m : beanExplorerService.buildMemberHints(current)) {
                    if (!"METHOD".equals(m.getKind())) {
                        continue;
                    }
                    if (propertyFieldNames.contains(m.getName())) {
                        continue;
                    }
                    hints.add(m);
                }
                sortMemberHints(hints);
                r.setHints(hints);
                return r;
            } catch (Exception ignored) {
            }
        }
        try {
            Map<String, Object> roots = buildRoots();
            Object v = expressionExecutor.evaluate(trimmed, roots);
            r.setValueTypeName(v == null ? "null" : v.getClass().getName());
            r.setHints(beanExplorerService.buildMemberHints(v));
        } catch (JavaLikeExpressionExecutor.ExpressionException e) {
            r.setError(e.getMessage());
            r.setHints(Collections.<MemberHint>emptyList());
        }
        return r;
    }

    private static boolean isSimpleFieldName(String name) {
        return name != null && SIMPLE_BEAN_PROPERTY_PATH.matcher(name).matches();
    }

    private static boolean isSimpleBeanPropertyPath(String s) {
        return s != null && SIMPLE_BEAN_PROPERTY_PATH.matcher(s.trim()).matches();
    }

    private static BeanInspectRequest buildInspectRequestForReceiver(String receiver) {
        String[] parts = receiver.trim().split("\\.", -1);
        BeanInspectRequest req = new BeanInspectRequest();
        req.setBeanName(parts[0]);
        List<PathStep> path = new ArrayList<PathStep>();
        for (int i = 1; i < parts.length; i++) {
            PathStep step = new PathStep();
            step.setKind("FIELD");
            step.setName(parts[i]);
            path.add(step);
        }
        req.setPath(path);
        return req;
    }

    private static List<MemberHint> hintsFromInspectChildren(List<BeanPropertyNode> children) {
        List<MemberHint> hints = new ArrayList<MemberHint>();
        if (children == null) {
            return hints;
        }
        for (BeanPropertyNode n : children) {
            MemberHint h = new MemberHint();
            h.setKind("PROPERTY");
            h.setName(n.getLabel());
            h.setTypeName(simpleTypeName(n.getClassName()));
            h.setSignature(n.getClassName());
            h.setValuePreview(n.getValuePreview());
            h.setExpandable(n.isExpandable());
            h.setNodeKind(n.getNodeKind());
            h.setInsertText(insertTextForInspectChild(n.getLabel()));
            if (StringUtils.hasText(n.getError())) {
                h.setSignature((n.getClassName() != null ? n.getClassName() + " " : "") + "(读取失败: " + n.getError() + ")");
            }
            hints.add(h);
        }
        return hints;
    }

    /**
     * Map 子节点 label 为 {key}，补全插入 ["key"]；List 为 [n]。
     */
    private static String insertTextForInspectChild(String label) {
        if (!StringUtils.hasText(label)) {
            return null;
        }
        if (label.length() >= 3 && label.charAt(0) == '[' && label.charAt(label.length() - 1) == ']') {
            return label;
        }
        if (label.length() >= 2 && label.charAt(0) == '{' && label.charAt(label.length() - 1) == '}') {
            String inner = label.substring(1, label.length() - 1);
            return "[" + jsonStringLiteral(inner) + "]";
        }
        return null;
    }

    private static String jsonStringLiteral(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    sb.append('\\').append(c);
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String simpleTypeName(String className) {
        if (!StringUtils.hasText(className)) {
            return "";
        }
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static void sortMemberHints(List<MemberHint> hints) {
        Collections.sort(hints, new Comparator<MemberHint>() {
            @Override
            public int compare(MemberHint a, MemberHint b) {
                int oa = kindOrder(a.getKind());
                int ob = kindOrder(b.getKind());
                if (oa != ob) {
                    return Integer.compare(oa, ob);
                }
                return String.valueOf(a.getName()).compareTo(String.valueOf(b.getName()));
            }
        });
    }

    private static int kindOrder(String k) {
        if ("PROPERTY".equals(k) || "FIELD".equals(k)) {
            return 0;
        }
        if ("METHOD".equals(k)) {
            return 1;
        }
        return 2;
    }

    private Map<String, Object> buildRoots() {
        ApplicationContext applicationContext = beanExplorerService.getApplicationContext();
        Map<String, Object> roots = new LinkedHashMap<String, Object>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            try {
                if (isValidVariableName(beanName)) {
                    roots.put(beanName, beanExplorerService.getBean(beanName));
                }
            } catch (Exception ignored) {
            }
        }
        ExecutionContext ctx = new ExecutionContext(applicationContext, beanExplorerService);
        roots.put("ctx", ctx);
        roots.put("applicationContext", applicationContext);
        return roots;
    }

    private boolean isValidVariableName(String name) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public BeanInspectResponse inspectResult(List<PathStep> path) {
        BeanInspectResponse response = new BeanInspectResponse();
        if (lastResult == null) {
            response.setError("请先执行表达式，再展开结果树");
            response.setChildren(Collections.emptyList());
            return response;
        }
        try {
            return beanExplorerService.inspectObjectAtPath(lastResult, path);
        } catch (Exception e) {
            response.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
            response.setChildren(Collections.emptyList());
            return response;
        }
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
