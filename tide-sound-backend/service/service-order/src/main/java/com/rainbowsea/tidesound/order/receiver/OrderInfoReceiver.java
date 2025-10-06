package com.rainbowsea.tidesound.order.receiver;


import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.util.MD5;
import com.rainbowsea.tidesound.order.service.MqOpsService;
import com.rainbowsea.tidesound.order.service.OrderInfoService;
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

import java.util.Map;

/**
 * 消息队列RabbitMQ 操作本地数据表
 * 消息队列导致分布式事务，本地消息表 +定时任务；消息没有被消费，
 * 就让生产者一直发，直到被消费者消费掉消息为止。利用本地消息表进行一个标记。
 */

@Component
@Slf4j
public class OrderInfoReceiver {

    @Autowired
    private MqOpsService mqOpsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderInfoService orderInfoService;


    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_LOCAL_MSG, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_LOCAL_MSG, durable = "true"),
            key = MqConst.ROUTING_LOCAL_MSG))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenUpdateLocalMsgStatus(String content, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;  // 不用消费
        }
        // 2.消费消息
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        // 消费成功，修改标记本地数据表，标记该消息被消费掉了
        mqOpsService.updateLocalMsgStatus(content);
        // 4.手动应答消息（将消息从队列中删除掉）
        channel.basicAck(deliveryTag, false);

    }


    /**
     * RabbitMQ 延时交换机——延时30分钟，关单
     * @param orderNo
     * @param message
     * @param channel
     */
    @RabbitListener(queues = {MqConst.QUEUE_CANCEL_ORDER})
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenCancelOrder(String orderNo, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(orderNo)) {
            return;  // 不用消费
        }
        // 2.消费消息
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        mqOpsService.cancelOrder(orderNo);
        // 4.手动应答消息（将消息从队列中删除掉）
        channel.basicAck(deliveryTag, false);

    }



    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_ORDER_PAY_SUCCESS, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER, durable = "true"),
            key = MqConst.ROUTING_ORDER_PAY_SUCCESS))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenWxPaySuccess(String content, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;  // 不用消费
        }
        // 2.处理消息
        Map<String, String> map = JSONObject.parseObject(content, Map.class);
        String orderNo = map.get("orderNo");
        String userId = map.get("userId");
        String msgMd5 = MD5.encrypt(orderNo);
        // 3.消费消息
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            orderInfoService.PaySuccess(Long.parseLong(userId), orderNo);  // 直接调用之前零钱支付成功的后续操作
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

}
