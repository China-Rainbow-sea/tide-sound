package com.rainbowsea.tidesound.search.receiver;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.util.MD5;
import com.rainbowsea.tidesound.search.service.MqOpsService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 处理上架下架，RabbitMQ 异步操作：同步到ES当中
 */

@Component
@Slf4j
public class SearchInfoListener {

    @Autowired
    private MqOpsService mqOpsService;

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * RabbitMQ 消费监听，上传专辑后(同步上架专辑)同步到ES当中
     * @param albumId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_ALBUM_UPPER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            key = MqConst.ROUTING_ALBUM_UPPER))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenAlbumInfoUpper(String albumId, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(albumId)) {
            return;  // 不用消费
        }
        // 2.处理消息
        String msgMd5 = MD5.encrypt(albumId);
        // 3.消费消息

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            mqOpsService.albumUpper(albumId);
            // 4.手动应答消息（将消息从队列中删除掉）
            channel.basicAck(deliveryTag, false);
        } catch (GuiguException e) {
            String msgRetryKey = "msg:retry:" + msgMd5;
            Long count = redisTemplate.opsForValue().increment(msgRetryKey);
            // 三次重试
            if (count >= 3) {
                log.error("消息已经到达了重试{}次，请人工排查错误原因：{}", count, e.getMessage());
                // 不能重试
                channel.basicNack(deliveryTag, false, false);
                redisTemplate.delete(msgRetryKey);
            } else {
                log.info("消息重试{}次", count);
                channel.basicNack(deliveryTag, false, true);
            }
        } catch (Exception e) {
            log.error("签收消息时网络出现了故障，异常原因：{}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }

    }


    /**
     * RabbitMQ 消费监听，下架删除专辑后(同步下架专辑)同步到ES当中
     * @param albumId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_ALBUM_LOWER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            key = MqConst.ROUTING_ALBUM_LOWER))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenAlbumInfoLower(String albumId, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(albumId)) {
            return;  // 不用消费
        }
        // 2.处理消息
        String msgMd5 = MD5.encrypt(albumId);
        // 3.消费消息

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            mqOpsService.albumLower(albumId);
            // 4.手动应答消息（将消息从队列中删除掉）
            channel.basicAck(deliveryTag, false);
        } catch (GuiguException e) {
            String msgRetryKey = "msg:retry:" + msgMd5;
            Long count = redisTemplate.opsForValue().increment(msgRetryKey);
            // 三次重试
            if (count >= 3) {
                log.error("消息已经到达了重试{}次，请人工排查错误原因：{}", count, e.getMessage());
                // 不能重试
                channel.basicNack(deliveryTag, false, false);
                redisTemplate.delete(msgRetryKey);
            } else {
                log.info("消息重试{}次", count);
                channel.basicNack(deliveryTag, false, true);
            }
        } catch (Exception e) {
            log.error("签收消息时网络出现了故障，异常原因：{}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }

    }


    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_ES_ALBUM_STAT, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ES_ALBUM_STAT, durable = "true"),
            key = MqConst.ROUTING_ES_ALBUM_STAT))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenAlbumStatUpdate(String content, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;  // 不用消费
        }


        JSONObject jsonObject = JSONObject.parseObject(content, JSONObject.class);

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        // 2.消费消息
        mqOpsService.updateAlbumStatNum(jsonObject);

        // 3.手动应答消息（将消息从队列中删除掉）
        channel.basicAck(deliveryTag, false);


    }


}
