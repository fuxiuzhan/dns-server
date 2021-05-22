package com.fxz.dnscore.annotation;



import com.fxz.dnscore.common.enums.CacheOpTypeEnum;
import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author xiuzhan.fu
 * cache注解的功能需要扩展
 * springcache有其缺陷，主要一点就是不能指定过期时间，也不支持基于LRU+Expr的过期策略，而且扩展本地缓存
 * 有一定困难
 * <p>
 * 为了加速访问，缓存可能采用二级缓存策略 redis+localcahe  query->localcache->redis->db
 * localcache采用小容量LRU+Expr淘汰策略缓存，redis采用expr淘汰策略
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface Cache {

    /**
     * 缓存主键 支持EL表达式，默认采用hashcode（方法+入参列表。。。）
     *
     * @return
     */
    String key() default "";

    /**
     * 缓存前缀
     *
     * @return
     */
    String keyPrefix() default "";

    String value() default "";

    /**
     * 缓存过期时间，启用本地缓存情况下两个缓存的超时时间一致
     *
     * @return
     */
    int expr() default 0;

    /**
     * 过期时间单位
     *
     * @return
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 缓存操作
     *
     * @return
     */
    CacheOpTypeEnum opType() default CacheOpTypeEnum.SAVE;

    /**
     * 是否启用本地缓存，本地缓存默认采用LRU+Expr淘汰策略
     *
     * @return
     */
    boolean localTurbo() default false;

    /**
     * 是否填充返回null值
     * @return
     */
    boolean includeNullResult() default false;
}

