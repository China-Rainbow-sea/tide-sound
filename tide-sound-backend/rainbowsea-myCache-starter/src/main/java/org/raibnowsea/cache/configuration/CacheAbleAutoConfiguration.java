package org.raibnowsea.cache.configuration;

import org.raibnowsea.cache.aspect.CacheAspect;
import org.raibnowsea.cache.constant.CacheAbleConstant;
import org.raibnowsea.cache.service.impl.CacheOpsServiceImpl;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 操作Redisson ，布隆过滤器，缓存
 */

//@Configuration
public class CacheAbleAutoConfiguration {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisProperties redisProperties;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 定义Redisson客户端的Bean对象
     */
    @Bean
    public RedissonClient redissonClient() {

        // 1.给Redisson的配置信息
        Config config = new Config();
        config.useSingleServer()
                .setPassword(redisProperties.getPassword())
                .setAddress(CacheAbleConstant.CACHE_REDIS_PROTOCOL + redisProperties.getHost() + CacheAbleConstant.CACHE_REDIS_PORT_SPLIT + redisProperties.getPort());
        // 2.创建Redisson客户端
        RedissonClient redissonClient = Redisson.create(config);

        return redissonClient;
    }


    /**
     * 定义一个BloomFilter的Bean对象
     */

    @Bean
    public RBloomFilter rBloomFilter(RedissonClient redissonClient) {
        // 1.如果在Redis中没有这个key,那么会创建一个key,并且将这个key对应的布隆过滤器对象返回 反之 直接将已经创建好的布隆过滤器返回给你。
        RBloomFilter<Object> albumIdBloomFilter = redissonClient.getBloomFilter(CacheAbleConstant.DISTRO_BLOOM_FILTER_NAME);
        String bloomFilterLockKey = CacheAbleConstant.DISTRO_BLOOM_FILTER_LOCK_KEY;
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(bloomFilterLockKey, CacheAbleConstant.DISTRO_BLOOM_FILTER_LOCK_VALUE);
        if (aBoolean) {
            // 2.初始化布隆过滤器
            boolean b = albumIdBloomFilter.tryInit(CacheAbleConstant.DISTRO_BLOOM_FILTER_EXCEPTED_INSERT, CacheAbleConstant.DISTRO_BLOOM_FILTER_FPP);  // 利用分布式锁保证分布式布隆的初始化只做一次
            if (b) {
                logger.info("分布式布隆过滤器初始化完，但是数据还未同步进去");
            } else {
                logger.info("分布式布隆过滤器已经初始化完毕");
            }
        }
        return albumIdBloomFilter;
    }


    /**
     * 定义缓存切面类组件(切面逻辑起作用)
     */

    @Bean
    public CacheAspect cacheAspect() {
        return new CacheAspect();
    }

    /**
     * 定义操作缓存的业务组件（用缓存组件操作缓存）
     */

    @Bean
    public CacheOpsServiceImpl cacheOpsService() {
        return new CacheOpsServiceImpl();
    }

}
