package com.rainbowsea.tidesound.live.config;

import com.alibaba.fastjson.JSON;
import com.rainbowsea.tidesound.model.live.SocketMsg;
import jakarta.websocket.Session;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiveRoomMediator {

    // 以直播间id作为key，以直播间用户id集合作为value
    private static Map<Long, List<Long>> userMap = new ConcurrentHashMap<>();
    // 以用户id作为key，以每一个用户的session作为value
    private static Map<Long, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 加入直播间的方法
     * @param roomId
     * @param userId
     * @param session
     */
    public static void join(Long roomId, Long userId, Session session){
        List<Long> userIds = userMap.get(roomId);
        if (userIds == null){
            userIds = new ArrayList<>();
            userMap.put(roomId, userIds); // 建立map和list集合的引用关系
        }
        userIds.add(userId); // 把当前用户放入直播间的用户列表中
        // 维护该用户对应的session
        sessionMap.put(userId, session);
    }

    /**
     * 离开直播间的方法
     * @param roomId
     * @param userId
     */
    public static void leave(Long roomId, Long userId){
        List<Long> userIds = userMap.get(roomId);
        if (userIds == null){
            userIds = new ArrayList<>();
            userMap.put(roomId, userIds); // 建立map和list集合的引用关系
        }
        userIds.remove(userId); // 把用户从直播间的用户列表中移除
        // 把该用户对应的session也移除
        sessionMap.remove(userId);
    }

    /**
     * 获取直播间的用户数
     * @param roomId
     * @return
     */
    public static Integer getCount(Long roomId){
        List<Long> userIds = userMap.get(roomId);
        if (userIds == null){
            return 0;
        }
        return userIds.size();
    }

    /**
     * 转发消息
     * @param msg
     */
    public static void sendMsg(SocketMsg msg){
        // 判断是否是心跳或者是无效消息，如果是则直接结束，不用转发
        if (msg == null || StringUtils.isBlank(msg.getMsgType())
                || msg.getMsgType().equals("0") || msg.getMsgType().equals("-1")){
            return;
        }

        // 根据直播间的id获取直播间用户列表
        List<Long> userIds = userMap.get(msg.getLiveRoomId());
        if (CollectionUtils.isEmpty(userIds)){ // 如果直播间没有用户则直接结束
            return;
        }
        // 遍历每一个用户，转发消息
        userIds.forEach(userId -> {
            sessionMap.get(userId).getAsyncRemote().sendText(JSON.toJSONString(msg));
        });
    }
}
