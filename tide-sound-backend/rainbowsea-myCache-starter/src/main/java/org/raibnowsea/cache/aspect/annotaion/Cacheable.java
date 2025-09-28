package org.raibnowsea.cache.aspect.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式缓存 + 分布式锁 + 布隆过滤器注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {


    String cacheKey() default "";// 定义缓存key

    String lockKey(); // 定义锁的key


    String bloomKey(); // 定义布隆过滤器的key


    boolean enableBloomFilter() default false; // 布隆开关


    boolean enableLock() default false;  // 锁的开关

}
