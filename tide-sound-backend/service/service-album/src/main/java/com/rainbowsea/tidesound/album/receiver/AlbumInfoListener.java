package com.rainbowsea.tidesound.album.receiver;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONAwareSerializer;

import com.rabbitmq.client.Channel;
import com.rainbowsea.tidesound.album.service.MqOpsService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.vo.album.TrackStatMqVo;
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
 *
 */

@Component
@Slf4j
public class AlbumInfoListener {


    @Autowired
    private MqOpsService mqOpsService;

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 消费消费
     * 对于队列 交换机以及绑定关系可以通过配置来指定 也可以通过下游 rabbit listern 注解指定
     * 下游引入了重试机制:
     *  如果重试次数在阈值之内，业务还有可能被进一步执行到，不会不执行了。如果重试次数达到了阈值，重试不会继续执行，人工干预错误原因。
     * @param content
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_TRACK_STAT_UPDATE, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK, durable = "true"),
            key = MqConst.ROUTING_TRACK_STAT_UPDATE))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenTrackStatTypeUpdate(String content, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;  // 不用消费
        }
        // 2.处理消息
        TrackStatMqVo trackStatMqVo = JSONObject.parseObject(content, TrackStatMqVo.class);

        // 3.消费消息
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            mqOpsService.trackStatTypeUpdate(trackStatMqVo);
            // 4.手动应答消息（将消息从队列中删除掉）
            channel.basicAck(deliveryTag, false);
        } catch (GuiguException e) {
            String msgRetryKey = "msg:retry:" + trackStatMqVo.getBusinessNo(); // 业务去重的编号
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
     * 监听：操作 更新专辑的购买量
     *
    void updateAlbumStatNum(JSONObject jsonObject);
     * @param content
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_ALBUM_STAT_UPDATE, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            key = MqConst.ROUTING_ALBUM_STAT_UPDATE))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenAlbumStatTypeUpdate(String content, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;  // 不用消费
        }


        JSONObject jsonObject = JSONObject.parseObject(content, JSONObject.class);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        // 2.消费消息
        mqOpsService.albumStatTypeUpdate(jsonObject);
        // 3.手动应答消息（将消息从队列中删除掉）
        channel.basicAck(deliveryTag, false);

    }


}
