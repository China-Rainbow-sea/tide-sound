package com.rainbowsea.tidesound.live.api;

import com.alibaba.fastjson.JSON;
import com.rainbowsea.tidesound.common.constant.RedisConstant;
import com.rainbowsea.tidesound.live.config.LiveRoomMediator;
import com.rainbowsea.tidesound.model.live.FromUser;
import com.rainbowsea.tidesound.model.live.SocketMsg;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 是一个多例的（同事类），而不是一个单例的
 */
@Controller
@ServerEndpoint("/api/websocket/{roomId}/{token}")
public class WebSocketApiController {







    private static RedisTemplate redisTemplate;

    @Autowired // 多例对象不能通过属性注入参数，需要通过set方法注入
    public void setRedisTemplate(RedisTemplate redisTemplate){
        WebSocketApiController.redisTemplate = redisTemplate;
    }


    /**
     * 建立连接
     * @param roomId
     * @param token
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam("roomId")Long roomId, @PathParam("token")String token, Session session){ // 加入中介者集合中
        System.out.println("建立连接！");



        // 根据 token查询登录用户的状态（UserInfoVo）
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        // 加入中介者集合中
        LiveRoomMediator.join(roomId, userInfoVo.getId(), session);

        // 通过中介者发送：xxx加入了直播间
        SocketMsg msg = new SocketMsg();
        msg.setLiveRoomId(roomId);
        msg.setMsgType(SocketMsg.MsgTypeEnum.JOIN_CHAT.getCode());
        msg.setMsgContent(userInfoVo.getNickname() + "加入了直播间！");
        // 构建消息发送者对象
        FromUser fromUser = new FromUser();
        BeanUtils.copyProperties(userInfoVo, fromUser);
        fromUser.setUserId(userInfoVo.getId());
        msg.setFromUser(fromUser);
        // 发送时间
        msg.setTime(new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date()));
        // LiveRoomMediator.sendMsg(msg);
        redisTemplate.convertAndSend(RedisConstant.LIVE_MESSAGE_CHANNEL, msg);
    }

    /**
     * 获取消息
     * @param msg
     */
    @OnMessage
    public void onMessage(String msg){ // 调用中介者转发消息
        System.out.println("收到了消息：" + msg);

        // 通过中介者转发消息
        // LiveRoomMediator.sendMsg(JSON.parseObject(msg, SocketMsg.class));
        // TODO：一定要报msg反序列化为SocketMsg对象
        redisTemplate.convertAndSend(RedisConstant.LIVE_MESSAGE_CHANNEL, JSON.parseObject(msg, SocketMsg.class));
    }


    /**
     * 关闭链接
     * @param roomId
     * @param token
     * @param session
     */
    @OnClose
    public void onClose(@PathParam("roomId")Long roomId, @PathParam("token")String token, Session session){ // 从中介者中移除
        System.out.println("关闭链接！");


        // 根据token查询登录用户的状态（UserInfoVo）
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        // 中介者中要移除当前用户
        LiveRoomMediator.leave(roomId, userInfoVo.getId());

        // 转发消息：xxx离开了直播间
        SocketMsg msg = new SocketMsg();
        msg.setLiveRoomId(roomId);
        msg.setMsgType(SocketMsg.MsgTypeEnum.CLOSE_SOCKET.getCode());
        msg.setMsgContent(userInfoVo.getNickname() + "离开了直播间！");
        // 构建消息发送者对象
        FromUser fromUser = new FromUser();
        BeanUtils.copyProperties(userInfoVo, fromUser);
        fromUser.setUserId(userInfoVo.getId());
        msg.setFromUser(fromUser);
        // 发送时间
        msg.setTime(new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date()));
        // LiveRoomMediator.sendMsg(msg);
        redisTemplate.convertAndSend(RedisConstant.LIVE_MESSAGE_CHANNEL, msg);
    }


    /**
     * 连接出错
     * @param ex
     */
    @OnError
    public void onError(Throwable ex){
        System.out.println("连接出错！" + ex.getMessage()); // 从中介者中移除
        ex.printStackTrace();
    }
}
