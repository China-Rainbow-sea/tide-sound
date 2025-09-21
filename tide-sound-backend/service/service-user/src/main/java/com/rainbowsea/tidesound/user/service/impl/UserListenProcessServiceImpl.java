package com.rainbowsea.tidesound.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.common.constant.SystemConstant;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.rabbit.service.RabbitService;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.common.util.MongoUtil;
import com.rainbowsea.tidesound.model.user.UserListenProcess;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import com.rainbowsea.tidesound.user.service.UserListenProcessService;
import com.rainbowsea.tidesound.vo.album.TrackStatMqVo;
import com.rainbowsea.tidesound.vo.user.UserListenProcessVo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;

    @Autowired
    private UserInfoService userInfoService;


    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private StringRedisTemplate redisTemplate;




    @Override
    public BigDecimal getTrackBreakSecond(Long trackId) {

        // 在@Tingshu 认证注解当中，同一个线程处理，获取当存储到线程当中的用户 id
        Long userId = AuthContextHolder.getUserId();

        // 1.构建条件对象 ES 查询
        Criteria criteria = Criteria.where("userId").is(userId).and("trackId").is(trackId);

        // 2.构建查询对象并且将条件对象放到查询对象中去
        Query query = new Query(criteria);

        // 3.利用查询对象
        /**
         * 两个参数
         * param1:query
         * param2:实体类
         * 三个参数：
         * param1:query
         * param2:实体类
         * param3:集合名（表名）
         *
         * 我们应用的要求：
         * 用户id:1-100:  1...100
         * 用户id:101-200 1...100   101%100=1 102%100=2 103%100=3 200%100=0
         *
         * 真正的表明：userListenProcess_1
         */
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if (userListenProcess == null) {
            // 这个声音以前没有播放过
            return new BigDecimal("0.00");
        }
        return userListenProcess.getBreakSecond();
    }

    @Override
    public void updateListenProcess(UserListenProcessVo userListenProcessVo) {

        Long userId = AuthContextHolder.getUserId();

        // 1.构建条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId());

        // 2.构建查询对象
        Query query = new Query(criteria);

        // 3.开始查询
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);

        if (userListenProcess == null) {
            // 保存声音的播放进度
            userListenProcess = new UserListenProcess();
            userListenProcess.setId(ObjectId.get().toString());
            userListenProcess.setUserId(userId);
            userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
            userListenProcess.setTrackId(userListenProcessVo.getTrackId());
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setIsShow(1); // 是否显示
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            UserListenProcess saveFlag = mongoTemplate.save(userListenProcess, collectionName);
            log.info("保存声音进度：{}", saveFlag != null ? "success" : "fail");
        } else {
            // 修改声音的播放进度
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setUpdateTime(new Date());
            UserListenProcess updateFlag = mongoTemplate.save(userListenProcess, collectionName);// 只有当文档id 存在的时候 save的作用是修改 反正则是新增一个文档
            log.info("修改声音进度：{}", updateFlag != null ? "success" : "fail");
        }


        // 更新MySQL中声音以及专辑的播放量（专辑0401  声音0701）
        // think:发送很多重复的消息给下游  下游重复监听到同一个消息的多次--->声音的播放量一直会增强
        // 对于同一个声音 在同一天 同一个用户不管听多少次 只让这个声音的播放量+1.
        // 两个维度：维度一：上游只发一次  下游就只会接收到一次
        //         维度二：上游无限发，下游做幂等性校验，只处理一次。
        // 锁机制：分布式锁实现（redis:setIfAbsent(String)、bitmap/mysql/zk/redisson）


        String bitMapKey = "user:track:" + new DateTime().toString("yyyy-MM-dd") + ":" + userListenProcessVo.getTrackId();
        Boolean aBoolean = redisTemplate.opsForValue().getBit(bitMapKey, userId);
        if (!aBoolean) {
            redisTemplate.opsForValue().setBit(bitMapKey, userId, true);
            redisTemplate.expire(bitMapKey, 1, TimeUnit.DAYS);  // 设置过期时间为 1 day
            TrackStatMqVo trackStatMqVo = userInfoService.prepareTrackStatMqDto(userListenProcessVo.getAlbumId(), userListenProcessVo.getTrackId(), SystemConstant.TRACK_STAT_PLAY, 1);
            rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE, JSONObject.toJSONString(trackStatMqVo));  //
        }
    }

    @Override
    public Map<Object, Object> getLatelyTrack() {

        Long userId = AuthContextHolder.getUserId();

        // 1.构建条件对象
        Criteria criteria = Criteria.where("userId").is(userId);

        // 1.1 封装排序条件
        Sort sort = Sort.by(Sort.Order.desc("updateTime"));

        // 2.构建查询对象
        Query query = new Query(criteria);
        query.with(sort);

        // 3.查询
        UserListenProcess userListenProcesses = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));

        if (userListenProcesses == null) {
            return null;
        }

        Map<Object, Object> map = new HashMap<>();
        map.put("albumId", userListenProcesses.getAlbumId());
        map.put("trackId", userListenProcesses.getTrackId());
        return map;
    }
}
