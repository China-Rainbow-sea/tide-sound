package com.rainbowsea.tidesound.order.receiver;


import com.rabbitmq.client.Channel;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.order.service.MqOpsService;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 消息队列RabbitMQ 操作本地数据表
 * 消息队列导致分布式事务，本地消息表 +定时任务；消息没有被消费，
 * 就让生产者一直发，直到被消费者消费掉消息为止。利用本地消息表进行一个标记。
 */

@Component
public class OrderInfoReceiver {

    @Autowired
    private MqOpsService mqOpsService;


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

}
