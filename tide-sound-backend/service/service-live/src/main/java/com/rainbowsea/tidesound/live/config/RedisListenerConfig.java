package com.rainbowsea.tidesound.live.config;

import com.rainbowsea.tidesound.common.constant.RedisConstant;
import com.rainbowsea.tidesound.model.live.SocketMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.Nullable;

@Configuration
public class RedisListenerConfig {

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * Redis 消息监听器绑定监听指定通道
     * 可以添加多个监听器，监听多个通道，只需要将消息监听器与订阅的通道/主题绑定即可。
     * @return
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory){
        // 初始化一个监听者容器
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        // 设置连接工厂，以连上redis
        listenerContainer.setConnectionFactory(connectionFactory);
        // 添加消息监听器，并监听对应的信道
        listenerContainer.addMessageListener((Message message, @Nullable byte[] pattern) -> {
            // 获取redis中的发布订阅的消息对象（不是SocketMsg），可以获取用户发送的二进制消息对象（SocketMsg），并且反序列化为一个SocketMsg对象
            SocketMsg msg = (SocketMsg)this.redisTemplate.getValueSerializer().deserialize(message.getBody());
            // 通过中介者转发消息，给自己服务器中的所有用户
            LiveRoomMediator.sendMsg(msg);
        }, new ChannelTopic(RedisConstant.LIVE_MESSAGE_CHANNEL));
        return listenerContainer;
    }
}
