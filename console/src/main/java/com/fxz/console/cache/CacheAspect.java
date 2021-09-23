package com.fxz.console.cache;//package com.fxz.dnscore.aspect;


import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Parameter;
import java.util.*;

/**
 * @author fxz
 */
@Slf4j
@Aspect
@Component
public class CacheAspect {


    @Value("${method.cache.enabled:true}")
    private boolean cacheEnabled;

    private static final String METHOD_CACHE_PREFIX = "";
    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    LruCache lruCache = new LruCache(1024);

    @Around("@annotation(com.fxz.console.cache.BatchCache) ||@annotation(com.fxz.console.cache.Cache)")
    public Object processCache(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (cacheEnabled) {

            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            List<Cache> saveList = new ArrayList<>();
            List<Cache> delList = new ArrayList<>();
            List<Cache> rawList = new ArrayList<>();
            Cache cache = methodSignature.getMethod().getAnnotation(Cache.class);
            BatchCache batchCache = methodSignature.getMethod().getAnnotation(BatchCache.class);
            try {
                if (Objects.nonNull(cache)) {
                    rawList.add(cache);
                }
                if (Objects.nonNull(batchCache)) {
                    if (batchCache.value().length > 0) {
                        for (Cache cache1 : batchCache.value()) {
                            rawList.add(cache1);
                        }
                    }
                }
                rawList.forEach(r -> {
                    if (r.opType().equals(CacheOpTypeEnum.SAVE)) {
                        saveList.add(r);
                    } else {
                        delList.add(r);
                    }
                });
                CacheValue result = getCache(proceedingJoinPoint, saveList);
                if (Objects.nonNull(result)) {
                    return result.getObject();
                }
                Object proceedResult = proceedingJoinPoint.proceed();
                setCache(proceedingJoinPoint, saveList, proceedResult);
                return proceedResult;
            } finally {
                delCache(proceedingJoinPoint, delList);
            }
        }
        return proceedingJoinPoint.proceed();
    }

    private CacheValue getCache(ProceedingJoinPoint proceedingJoinPoint, List<Cache> cacheList) {
        if (Objects.nonNull(cacheList) && cacheList.size() > 0) {
            for (Cache cache : cacheList) {
                String key = evaluateExpression(proceedingJoinPoint, cache);
                if (cache.localTurbo()) {
                    Object o = lruCache.get(key);
                    if (o != null && o instanceof CacheValue) {
                        CacheValue localCacheValue = (CacheValue) o;
                        if (localCacheValue.getExprInSeconds() > 0) {
                            if ((System.currentTimeMillis() - localCacheValue.getLastAccessTime()) > localCacheValue.getExprInSeconds() * 1000L) {
                                lruCache.remove(key);
                            } else {
                                return localCacheValue;
                            }
                        } else {
                            return localCacheValue;
                        }
                    }
                } else {
                    if (Objects.nonNull(redisTemplate)) {
                        Object o = redisTemplate.opsForValue().get(key);
                        if (Objects.nonNull(o) && o instanceof String) {
                            return JSON.parseObject(o + "", CacheValue.class);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void setCache(ProceedingJoinPoint proceedingJoinPoint, List<Cache> cacheList, Object result) {
        if (Objects.nonNull(cacheList) && cacheList.size() > 0) {
            for (Cache cache : cacheList) {
                String key = evaluateExpression(proceedingJoinPoint, cache);
                CacheValue cacheValue = new CacheValue();
                cacheValue.setLastAccessTime(System.currentTimeMillis());
                cacheValue.setExprInSeconds(cache.unit().toSeconds(cache.expr()));
                cacheValue.setObject(result);
                if (cache.localTurbo()) {
                    if ((cache.includeNullResult() && Objects.isNull(result)) || (!cache.includeNullResult() && Objects.nonNull(result))) {
                        lruCache.put(key, cacheValue);
                    }
                }
                if (Objects.nonNull(redisTemplate) && ((cache.includeNullResult() && Objects.isNull(result)) || (!cache.includeNullResult() && Objects.nonNull(result)))) {
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(cacheValue), cache.expr(), cache.unit());
                }
            }
        }
    }

    private void delCache(ProceedingJoinPoint proceedingJoinPoint, List<Cache> cacheList) {
        if (Objects.nonNull(cacheList) || cacheList.size() > 0) {
            cacheList.stream().forEach(singleCache -> {
                String key = evaluateExpression(proceedingJoinPoint, singleCache);
                if (singleCache.localTurbo()) {
                    lruCache.remove(key);
                }
                if (Objects.nonNull(redisTemplate)) {
                    redisTemplate.delete(key);
                }
            });
        }
    }

    private String evaluateExpression(ProceedingJoinPoint proceedingJoinPoint, Cache cache) {
        if (Objects.nonNull(cache) && StringUtils.hasText(cache.key())) {
            try {
                EvaluationContext context = new StandardEvaluationContext();
                ExpressionParser parser = new SpelExpressionParser();
                MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
                Parameter[] ps = methodSignature.getMethod().getParameters();
                if (ps != null) {
                    for (int j = 0, l = ps.length; j < l; ++j) {
                        context.setVariable(ps[j].getName(), proceedingJoinPoint.getArgs()[j]);
                    }
                }
                String keyPrefix = cache.value();
                String value = parser.parseExpression(cache.key()).getValue(context, String.class);
                return keyPrefix + value;
            } catch (Exception e) {
                log.warn("cache annotation expression error using default key instead，method->{}, error->{}", proceedingJoinPoint.getSignature().getName(), e);
            }
        }
        return defaultKey(proceedingJoinPoint);
    }

    private String defaultKey(ProceedingJoinPoint proceedingJoinPoint) {
        String className =proceedingJoinPoint.getSignature().getDeclaringTypeName();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        String value = METHOD_CACHE_PREFIX + "_" + className + "_" + methodSignature.getName() + "_" + proceedingJoinPoint.getArgs().length + "_" + Arrays.deepHashCode(proceedingJoinPoint.getArgs());
        return value;
    }
}

@Data
class CacheValue {
    private Object object;
    private long lastAccessTime;
    private long exprInSeconds;
}

class LruCache extends LinkedHashMap {
    int size;

    LruCache(int size) {
        super(size);
        this.size = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > size;
    }
}