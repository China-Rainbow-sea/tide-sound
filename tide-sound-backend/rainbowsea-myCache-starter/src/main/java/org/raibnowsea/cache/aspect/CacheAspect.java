package org.raibnowsea.cache.aspect;

import com.fasterxml.jackson.core.type.TypeReference;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.raibnowsea.cache.aspect.annotaion.Cacheable;
import org.raibnowsea.cache.constant.CacheAbleConstant;
import org.raibnowsea.cache.service.CacheOpsService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * 分布式缓存 + 分布式锁 + 布隆过滤器注解 的业务逻辑具体处理
 */
//@Component
@Aspect
public class CacheAspect {

    @Autowired
    private CacheOpsService cacheOpsService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter rBloomFilter;


    /**
     * 切面逻辑
     * 1.查询缓存
     * 2.回源
     * 3.同步到缓存中
     * 环绕通知最合适
     * <p>
     * 适配任意一个方法（通用）
     */
    @Around(value = "@annotation(org.raibnowsea.cache.aspect.annotaion.Cacheable)")
    public Object cacheCheck(ProceedingJoinPoint pjp) throws Throwable {

        // 1.获取目标方法的指定注解对象
        Cacheable cacheable = getMethodAnnotaion(pjp, Cacheable.class);

        // 2.获取目标方法带泛型的返回值类型
        Type genericReturnType = getMethodGerenicReturnType(pjp);
        // 3.定义变量
        // 3.1 定义缓存key表达式
        String cacheKeyExpression = cacheable.cacheKey();
        // 3.2  计算缓存key
        String cacheKey = dynamicComputeKey(cacheKeyExpression, pjp, String.class);
        // 3.3 定义锁key表达式
        String lockKeyExpression = cacheable.lockKey();
        // 3.4 计算锁的key
        String lockKey = dynamicComputeKey(lockKeyExpression, pjp, String.class);
        // 3.5 定义布隆过滤器的key表达式
        String bloomKeyExpression = cacheable.bloomKey();
        // 3.6 计算布隆过滤器的key
        Long bloomKey = dynamicComputeKey(bloomKeyExpression, pjp, Long.class);
        // 3.7 获取分布式布隆过滤器的开关
        boolean enableBloomFilter = cacheable.enableBloomFilter();
        // 3.8 获取分布式锁的开关
        boolean enableLockFlag = cacheable.enableLock();

        // 4.使用布隆过滤器
        if (enableBloomFilter) {
            if (!rBloomFilter.contains(bloomKey)) {
                return null;
            }
        }

        // 5.没有使用布隆过滤器直接查询缓存
        Object dataFromCache = cacheOpsService.getDataFromCache(cacheKey, new TypeReference<Object>() {
            @Override
            public Type getType() {
                return genericReturnType;
            }
        });

        // 6.缓存命中
        if (dataFromCache != null) {
            return dataFromCache;
        }

        // 7.缓存未命中且没有开启分布式锁
        if (!enableLockFlag) {
            // 7.1.回源
            Object proceed = pjp.proceed(); // 执行目标方法
            // 7.2 同步数据到缓存中
            cacheOpsService.saveDataToCache(cacheKey, proceed);
            // 7.3 返回数据
            return proceed;
        }


        // 8.开启分布式锁 获取锁对象
        RLock lock = redissonClient.getLock(lockKey);
        // 9.抢锁
        boolean acquireLock = lock.tryLock();
        // 10.抢锁成功
        if (acquireLock) {
            try {
                // 11.回源
                Object proceed = pjp.proceed(); // 执行目标方法

                // 12.同步数据到缓存中
                cacheOpsService.saveDataToCache(cacheKey, proceed);

                // 13.返回数据
                return proceed;
            } finally {
                lock.unlock();// 释放锁
            }
        } else {
            // 14.抢锁失败
            Thread.sleep(CacheAbleConstant.DATA_SYNC_TTL); // 压测给一个精准值

            // 15.查询缓存
            Object result = cacheOpsService.getDataFromCache(cacheKey, new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return genericReturnType;
                }
            });
            // 16. 缓存命中
            if (result != null) {
                return result;
            }
            // 17. 兜底继续回源
            return pjp.proceed(); // 执行目标方法
        }
    }

    private static Type getMethodGerenicReturnType(ProceedingJoinPoint pjp) {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        Type genericReturnType = method.getGenericReturnType();
        return genericReturnType;
    }


    /**
     * 根据表达式获取key
     *
     * @param cacheKeyExpression
     * @param pjp
     * @param resultClass
     * @return
     */
    private <T> T dynamicComputeKey(String cacheKeyExpression, ProceedingJoinPoint pjp, Class<T> resultClass) {

        // 1.创建表达式解析器对象
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

        // 2.创建计算上下文对象
        StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
        standardEvaluationContext.setVariable("args", pjp.getArgs());

        // 3.创建解析模版对象
        TemplateParserContext templateParserContext = new TemplateParserContext();

        // 4.解析表达式
        Expression expression = spelExpressionParser.parseExpression(cacheKeyExpression, templateParserContext);

        // 5.获取计算之后的值

        T value = expression.getValue(standardEvaluationContext, resultClass);

        // 6.缓存key的值返回

        return value;


    }


    /**
     * 获取目标方法的指定类型注解
     *
     * @param pjp
     * @param tClass
     * @param <T>
     * @return
     */

    private static <T extends Annotation> T getMethodAnnotaion(ProceedingJoinPoint pjp, Class<T> tClass) {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();

        Method method = methodSignature.getMethod();
        T annotation = (T) method.getAnnotation(tClass);
        return annotation;
    }


}
