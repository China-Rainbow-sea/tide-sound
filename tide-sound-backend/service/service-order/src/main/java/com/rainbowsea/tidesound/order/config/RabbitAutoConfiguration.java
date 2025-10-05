package com.rainbowsea.tidesound.order.config;


import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 绑定延时插件-延时交换机
 */

@Configuration
public class RabbitAutoConfiguration {


    /**
     * 定义延时交换机
     * <p>
     * String name,:交换机的名字
     * String type,：交换机类型
     * boolean durable：交换机是否持久化（rabbitmq服务重启之后交换机的名字 类型 等元数据是否还存在）
     * boolean autoDelete,交换机是否自动删除。（如果该交换机没有队列与其绑定，这个交换机就没有）
     * Map<String, Object> arguments：交换机的参数
     * <p>
     * rabbitMQ的路由策略：
     * <p>
     * direct:点对点：一对一
     * fanout:广播：一对多
     * topic:通配【路由键可以灵活的指定 [* ?]】
     */
    @Bean
    public CustomExchange customExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");   // key: x-delayed-type  value:direct

        return new CustomExchange(MqConst.EXCHANGE_CANCEL_ORDER, "x-delayed-message", true, false, args);

    }


    /**
     * 定义正常的队列
     */
    @Bean
    public Queue delayQueue() {
        return new Queue(MqConst.QUEUE_CANCEL_ORDER, true, false, false);
    }


    /**
     * 定义延时交换机和正常队列的绑定
     */

    @Bean
    public Binding delayBinding(@Autowired CustomExchange customExchange, @Autowired Queue delayQueue) {
        return BindingBuilder.bind(delayQueue).to(customExchange).with(MqConst.ROUTING_CANCEL_ORDER).noargs();
    }


}
