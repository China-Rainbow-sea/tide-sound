package com.rainbowsea.tidesound.cdc.handler;

import com.rainbowsea.tidesound.cdc.entity.CdcEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * canal监听操作
 */
@Component
@CanalTable("album_info") // 监听变更的数据表名
@Slf4j
public class CdcEntityHandler implements EntryHandler<CdcEntity> {

    @Autowired

    private StringRedisTemplate redisTemplate;

//    @Autowired
//    private RabbitService rabbitService;

    /**
     * album_info表中有数据新增的时候 会回调该方法
     *
     * @param cdcEntity
     */

    @Override
    public void insert(CdcEntity cdcEntity) {
        log.info("canal客户端监听到了album_info表中有数据的新增的id：{}", cdcEntity.getId());
    }

    /**
     * album_info表中有有数据发生了修改，。会回调该方法
     * CdcEntity before：修改之前的老数据
     * CdcEntity after：修改后的新数据
     *
     * @param before
     * @param after
     */
    @Override
    public void update(CdcEntity before, CdcEntity after) {
        log.info("canal客户端监听到了album_info表中有数据的修改，修改之前的id：{}", before.getId());
        log.info("canal客户端监听到了album_info表中有数据的修改，修改之后的id：{}", after.getId());

        String cacheKey = "cache:info:" + after.getId();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

        try {
            scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    redisTemplate.delete(cacheKey);
                }
            }, 300, TimeUnit.MICROSECONDS);

        } catch (Exception e) {
            //  TODO  重试删除操作
//            rabbitService.sendMessage()//

        }
    }


    /**
     * album_info表中有有数据发生了删除，会回调该方法
     * CdcEntity cdcEntity：删除的对象是哪一个
     *
     * @param cdcEntity
     */

    @Override
    public void delete(CdcEntity cdcEntity) {
        log.info("canal客户端监听到了album_info表中有数据的删除，删除的数据的id：{}", cdcEntity.getId());
    }
}
