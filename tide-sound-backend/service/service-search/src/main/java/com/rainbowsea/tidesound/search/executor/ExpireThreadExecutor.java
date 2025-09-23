package com.rainbowsea.tidesound.search.executor;

import com.rainbowsea.tidesound.common.constant.RedisConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 执行器管理线程对redis中的锁 key续期
 */

public class ExpireThreadExecutor {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private Long ttl;
    private TimeUnit timeUnit;
    private StringRedisTemplate redisTemplate;
    private Long taskId;
    static ScheduledExecutorService scheduledExecutorService;
    static ScheduledFuture<?> scheduledFuture = null;


    static {
        // 防止一个线程，续期失败，这里我们使用2个线程，进行续期
        scheduledExecutorService = Executors.newScheduledThreadPool(2);

    }

    public ExpireThreadExecutor(StringRedisTemplate redisTemplate, Long taskId) {
        this.redisTemplate = redisTemplate;
        this.taskId = taskId;
    }

    /**
     * 定义一个续期方法
     */
    public void renewal(Long ttl, TimeUnit timeUnit) {

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                logger.info("续期线程开始续期:");
                // 续期
                redisTemplate.expire(RedisConstant.ALBUM_LOCK_SUFFIX + taskId, ttl, timeUnit);
            }
        }, ttl / 3, ttl / 3, TimeUnit.SECONDS);  // 续期时间 ttl 这里设置的是30，也就是10秒续期
    }

    /**
     * 中断续期任务方法
     */
    public Boolean cancelRenewal() {
        boolean cancel = scheduledFuture.cancel(true);  // 取消给线程池的任务
        logger.info("续期线程结束续期:");
        return cancel;
    }
}
