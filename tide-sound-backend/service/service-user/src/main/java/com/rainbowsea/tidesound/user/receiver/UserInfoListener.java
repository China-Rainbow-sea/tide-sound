package com.rainbowsea.tidesound.user.receiver;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.util.MD5;
import com.rainbowsea.tidesound.user.service.MqOpsService;
import com.rainbowsea.tidesound.vo.user.UserPaidRecordVo;
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


@Component
@Slf4j
public class UserInfoListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MqOpsService mqOpsService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_USER_PAY_RECORD, durable = "true"), exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"), key = MqConst.ROUTING_USER_PAY_RECORD))
    @SneakyThrows   // SneakyThrows可以绕开编译时候的异常 但是真正在运行期间出现异常依然会抛出来
    public void listenUserPiadRecored(String content, Message message, Channel channel) {


        // 1.判断消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;  // 不用消费
        }

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        // 2.处理消息
        UserPaidRecordVo userPaidRecordVo = JSONObject.parseObject(content, UserPaidRecordVo.class);
        // 3.消费消息
        try {
            mqOpsService.listenUserPiedRecord(userPaidRecordVo);

            // 4.手动应答消息（将消息从队列中删除掉）
            channel.basicAck(deliveryTag, false);
        } catch (GuiguException e) {
            String md5MsgStr = MD5.encrypt(content);
            String msgRetryKey = "msg:retry:" + md5MsgStr;
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
