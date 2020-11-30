package com.example.cache.annotation;

import java.lang.annotation.*;

/**
 * 批量存入redis缓存注解
 *
 * @author zhuwei
 * @Date 2020/10/27 PM5:09
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BatchCache {

    /**
     * 缓存名称前缀
     *
     * @return
     */
    String value() default "";

    /**
     * key值
     */
    String key() default "id";

    /**
     * 缓存过期时间
     *
     * @return
     */
    long seconds() default 180L;

    /**
     * 缓存后缀 映射字段 (类型为Collection）
     *
     * @return
     */
    String keyMapper() default "id";
}
