package com.rainbowsea.tidesound.account.receiver;

import com.rabbitmq.client.Channel;
import com.rainbowsea.tidesound.account.service.MqOpsService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.util.MD5;
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

import java.util.concurrent.TimeUnit;

/**
 * Description:
 */

@Slf4j
@Component
public class AccountInfoListener {


    // 操作 Redis 默认是key-value 是 String 类型
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 操作 rabbitMQ
    @Autowired
    private MqOpsService mqOpsService;


    /**
     * 接收 service-user 微服务 当中初次登录注册用户时，同步初始化该用户的账户信息
     * 通过 rabbitMQ消息队列的方式,进行初始化该用户的账户信息
     * @param content   service-user 微服务(方的RabbitMQ消息队列，发送过来的) 直接的 userId
     * @param message   service-user 微服务(方的RabbitMQ消息队列，发送过来的) 被封装后的消息内容(封装了 userId)  message 包含了 content 当中的内容
     * @param channel  消息通道
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"),
            key = MqConst.ROUTING_USER_REGISTER))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenUserAccountRegister(String content, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;  // 不用消费
        }
        // 2.处理消息
        String msgMd5 = MD5.encrypt(content);
        // 3.消费消息
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        // 通过 redis 进行分布式,记录重试次数,
        String msgRetryKey = "msg:retry:" + msgMd5;
        try {
            // 消费/处理消息队列当中的消息，可能发生异常
            mqOpsService.userAccountRegister(content);  // 消费消息,初始化该用户账户余额信息
            // 4. 消息消费成功,手动应答消息（将消息从队列中删除掉）
            channel.basicAck(deliveryTag, false);
        } catch (GuiguException e) {

            // Redis  记录重试次数
            Long count = redisTemplate.opsForValue().increment(msgRetryKey); // i++

            // 如果是第一次创建这个key（count == 1），为其设置过期时间
            if (count != null && count == 1) {
                redisTemplate.expire(msgRetryKey, 1, TimeUnit.DAYS); // 设置 1天后过期
            }
            // 三次重试
            if (count >= 3) {
                log.error("消息已经到达了重试{}次，请人工排查错误原因：{}", count, e.getMessage());
                // 不能重试
                channel.basicNack(deliveryTag, false, false);
                redisTemplate.delete(msgRetryKey); // 同时从 redis 记录的次数删除
            } else {
                // 重试次数,没有超过 3 次, 让RabbitMQ 消息重发 true,重新接受消费消息
                log.info("消息重试{}次", count);
                channel.basicNack(deliveryTag, false, true);
            }
        } catch (Exception e) {

            // 无论何种原因导致消息最终被丢弃，都应该尝试清理重试计数键
            // 如果key不存在，delete操作是安全的，不会抛异常
            redisTemplate.delete(msgRetryKey);
            log.error("签收消息时网络出现了故障，异常原因：{}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }

    }
}