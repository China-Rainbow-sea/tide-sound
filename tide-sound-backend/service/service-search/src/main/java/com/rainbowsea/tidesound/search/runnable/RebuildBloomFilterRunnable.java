package com.rainbowsea.tidesound.search.runnable;


import com.rainbowsea.tidesound.search.factory.ScheduleTaskThreadPoolFactory;
import com.rainbowsea.tidesound.search.service.impl.ItemServiceImpl;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 执行定时任务类，该定时任务 run 中重建布隆过滤器(更新分布式布隆过滤器数据)
 */

public class RebuildBloomFilterRunnable implements Runnable {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private RedissonClient redissonClient;

    private StringRedisTemplate redisTemplate;

    private ItemServiceImpl itemServiceImpl;

    public RebuildBloomFilterRunnable(RedissonClient redissonClient, StringRedisTemplate redisTemplate, ItemServiceImpl itemServiceImpl) {

        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
        this.itemServiceImpl = itemServiceImpl;
    }


    @Override
    public void run() {
        RBloomFilter<Object> albumIdBloomFilterNew = redissonClient.getBloomFilter("albumIdBloomFilterNew");

        albumIdBloomFilterNew.tryInit(1000000l, 0.001);

        List<Long> albumInfoIdList = itemServiceImpl.getAlbumInfoIdList();   // 重数据中行查询
        for (Long albumId : albumInfoIdList) {
            albumIdBloomFilterNew.add(albumId);
        }
        albumIdBloomFilterNew.add(2000L);// 测试的时候手动添加的。

        // rename key  key1  用key1的名字换key的名字（反向操作）

        String script = " redis.call(\"del\",KEYS[1])" +
                "  redis.call(\"del\",KEYS[2])" +
                "  redis.call(\"rename\",KEYS[3],KEYS[1])" +
                "  redis.call(\"rename\",KEYS[4],KEYS[2]) return 0";
        List<String> asList = Arrays.asList("albumIdBloomFilter", "{albumIdBloomFilter}:config", "albumIdBloomFilterNew", "{albumIdBloomFilterNew}:config");
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), asList);
        if (execute == 0) {
            logger.info("老布隆已经被删除，新布隆上线...");
        }

//        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
//
//        scheduledExecutorService.schedule(this,10, TimeUnit.SECONDS);   // 延时任务+嵌套任务本身 从而实现定时的效果。【nacos源码】

        ScheduleTaskThreadPoolFactory instance = ScheduleTaskThreadPoolFactory.getINSTANCE();
        //instance.execute(this, 20l, TimeUnit.SECONDS);// 测试使用的, 每20L执行，更新布隆过滤器
        instance.execute(this, 7l, TimeUnit.DAYS);// 线上使用的, 每 7 day 执行更新布隆过滤器
    }
}
