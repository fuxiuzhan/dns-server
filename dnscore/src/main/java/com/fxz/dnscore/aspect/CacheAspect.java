//package com.fxz.dnscore.aspect;
//
//
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.expression.EvaluationContext;
//import org.springframework.expression.ExpressionParser;
//import org.springframework.expression.spel.standard.SpelExpressionParser;
//import org.springframework.expression.spel.support.StandardEvaluationContext;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.lang.reflect.Parameter;
//import java.util.LinkedHashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.TimeUnit;
//
///**
// */
//@Component
//@Aspect
//@Slf4j
//public class CacheAspect {
//
//    @Value("${method.cache.enabled:true}")
//    private boolean cacheEnabled;
//
//    @Autowired
//    @Qualifier("redisTemplate")
//    private RedisTemplate redisTemplate;
//
//    static LruCache lruCache = new LruCache(1024);
//
//    @Around("@annotation(Cache)")
//    public Object processCache(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
//        /**
//         * 实现二级缓存，缓存失效？
//         */
//        if (cacheEnabled) {
//            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
//            Cache cacheAnno = methodSignature.getMethod().getAnnotation(Cache.class);
//            String key = getKeyExpression(proceedingJoinPoint, cacheAnno);
//            log.info("cache annotation key expression->{}, key expression value->{}", cacheAnno.key(), key);
//            Object cacheObject = getObject(key, cacheAnno);
//            if (!Objects.isNull(cacheObject)) {
//                return cacheObject;
//            }
//            int expr = cacheAnno.expr();
//            TimeUnit timeUnit = cacheAnno.unit();
//            Object result = proceedingJoinPoint.proceed();
//            setObject(key, result, expr, timeUnit, cacheAnno);
//            return result;
//        } else {
//            return proceedingJoinPoint.proceed();
//        }
//    }
//
//    private void setObject(String key, Object result, long expr, TimeUnit timeUnit, Cache cacheAnno) {
//        if (cacheAnno.opType() == CacheOpTypeEnum.DELETE) {
//            lruCache.remove(key);
//            redisTemplate.delete(key);
//            log.info("cache delete key->{}", key);
//            return;
//        }
//        if (result != null) {
//            if (cacheAnno.localTurbo()) {
//                /**
//                 * 组装本地缓存
//                 */
//                LocalCacheValue localCacheValue = new LocalCacheValue();
//                localCacheValue.expr = expr;
//                localCacheValue.lastAccessTime = System.currentTimeMillis();
//                localCacheValue.object = result;
//                localCacheValue.timeUnit = timeUnit;
//                lruCache.put(key, localCacheValue);
//            }
//            if (expr > 0) {
//                redisTemplate.opsForValue().set(key, result, expr, timeUnit);
//            } else {
//                redisTemplate.opsForValue().set(key, result);
//            }
//        }
//    }
//
//    private Object getObject(String key, Cache cacheAnno) {
//        if (cacheAnno.opType() == CacheOpTypeEnum.DELETE) {
//            return null;
//        }
//        /**
//         * 检查localcache
//         */
//        if (cacheAnno.localTurbo()) {
//            /**
//             * 从本地读取
//             */
//            Object o = lruCache.get(key);
//            if (o != null && o instanceof LocalCacheValue) {
//                /**
//                 * 检查过期时间，如果过期直接移除，惰性移除
//                 */
//                LocalCacheValue localCacheValue = (LocalCacheValue) o;
//                if (localCacheValue.getExpr() > 0) {
//                    if (System.currentTimeMillis() - localCacheValue.getLastAccessTime() > localCacheValue.getExpr() * convertUnit(localCacheValue.timeUnit) * 1000L) {
//                        lruCache.remove(key);
//                    } else {
//                        return localCacheValue.getObject();
//                    }
//                } else {
//                    return localCacheValue.getObject();
//                }
//            }
//        }
//        Object cacheObject = redisTemplate.opsForValue().get(key);
//        return cacheObject;
//    }
//
//    private int hashCode(String clazz, String method, Object[] params) {
//        int paramsHashCode = 0;
//        if (params != null && params.length > 0) {
//            for (int i = 0; i < params.length; i++) {
//                if (params[i] != null) {
//                    paramsHashCode = paramsHashCode | GsonUtil.toJson(params[i]).hashCode();
//                }
//            }
//        }
//        int clazzHashCode = StringUtils.isEmpty(clazz) ? 0 : clazz.hashCode();
//        int methodHashCode = StringUtils.isEmpty(method) ? 0 : method.hashCode();
//        return Math.abs(clazzHashCode | methodHashCode | paramsHashCode);
//    }
//
//
//    private String getKeyExpression(ProceedingJoinPoint proceedingJoinPoint, Cache cacheAnno) {
//        String keyPrefix = Constant.METHOD_CACHE_PREFIX;
//        if (!StringUtils.isEmpty(cacheAnno.key())) {
//            try {
//                EvaluationContext context = new StandardEvaluationContext();
//                ExpressionParser parser = new SpelExpressionParser();
//                MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
//                Parameter[] ps = methodSignature.getMethod().getParameters();
//                if (ps != null) {
//                    for (int j = 0, l = ps.length; j < l; ++j) {
//                        context.setVariable(ps[j].getName(), proceedingJoinPoint.getArgs()[j]);
//                    }
//                }
//                if (!StringUtils.isEmpty(cacheAnno.keyPrefix())) {
//                    keyPrefix = cacheAnno.keyPrefix();
//                }
//                String value = parser.parseExpression(cacheAnno.key()).getValue(context, String.class);
//                return keyPrefix + value;
//            } catch (Exception e) {
//                log.warn("cache annotation expression error using defaultkey instead，method->{}, error->{}", proceedingJoinPoint.getSignature().getName(), e);
//            }
//        }
//        return defaultKey(proceedingJoinPoint);
//    }
//    public static long convertUnit(TimeUnit timeUnit) {
//        switch (timeUnit) {
//            case MINUTES:
//                return 60L;
//            case HOURS:
//                return 60 * 60L;
//            case DAYS:
//                return 24 * 60 * 60L;
//            default:
//                return 1L;
//        }
//    }
//    private String defaultKey(ProceedingJoinPoint proceedingJoinPoint) {
//        String className = proceedingJoinPoint.getTarget().getClass().getName();
//        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
//        String value = Constant.METHOD_CACHE_PREFIX + "_" + methodSignature.getName() + "_" + hashCode(className, methodSignature.getName(), proceedingJoinPoint.getArgs()) + "";
//        return value;
//    }
//
//    @Data
//    class LocalCacheValue {
//        private Object object;
//        private long lastAccessTime;
//        private TimeUnit timeUnit;
//        private long expr;
//    }
//
//    static class LruCache extends LinkedHashMap {
//        int size = 1024;
//
//        LruCache(int size) {
//            super(size);
//            this.size = size;
//        }
//
//        @Override
//        protected boolean removeEldestEntry(Map.Entry eldest) {
//            return size() > size;
//        }
//    }
//}
