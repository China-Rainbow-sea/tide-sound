package com.rainbowsea.tidesound.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.constant.PublicConstant;
import com.rainbowsea.tidesound.common.constant.RedisConstant;
import com.rainbowsea.tidesound.common.constant.SystemConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.rabbit.service.RabbitService;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.common.util.MongoUtil;
import com.rainbowsea.tidesound.model.user.UserCollect;
import com.rainbowsea.tidesound.model.user.UserInfo;
import com.rainbowsea.tidesound.model.user.UserPaidAlbum;
import com.rainbowsea.tidesound.model.user.UserPaidTrack;
import com.rainbowsea.tidesound.model.user.UserSubscribe;
import com.rainbowsea.tidesound.user.mapper.UserInfoMapper;
import com.rainbowsea.tidesound.user.mapper.UserPaidAlbumMapper;
import com.rainbowsea.tidesound.user.mapper.UserPaidTrackMapper;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.vo.album.TrackListVo;
import com.rainbowsea.tidesound.vo.album.TrackStatMqVo;
import com.rainbowsea.tidesound.vo.user.UserCollectVo;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    // 微信小程序SDK服务,授权的登录认证
    @Autowired
    private WxMaService wxMaService;


    // 获取token 加密的私钥和公钥
    @Autowired
    private RsaSigner rsaSigner;


    // 操作 Redis
    @Autowired
    private StringRedisTemplate redisTemplate;


    // 操作 RabbitMQ
    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private MongoTemplate mongoTemplate;


    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Override
    public TrackStatMqVo prepareTrackStatMqDto(Long albumId, Long trackId, String trackStatType, int count) {
        TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
        trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replace("-", "")); // 消息去重
        trackStatMqVo.setAlbumId(albumId);
        trackStatMqVo.setTrackId(trackId);
        trackStatMqVo.setStatType(trackStatType);
        trackStatMqVo.setCount(count);
        return trackStatMqVo;
    }

    /**
     * 返回微信登录成功后的 Map ,Map 当中存放了JWT认证的 token 信息
     *
     * @param code
     * @return
     */
    @Override
    public Map<String, Object> wxLogin(String code) {
        // 1. 判断 code 码是否存在
        if (StringUtils.isEmpty(code)) {
            throw new GuiguException(201, "code 不存在");
        }


        // 2. 调用微信服务端
        String openid = "";
        WxMaUserService userService = wxMaService.getUserService();
        WxMaJscode2SessionResult sessionInfo = null;
        try {
            sessionInfo = userService.getSessionInfo(code);
            openid = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            log.error("调用微信服务端失败:{}", e.getMessage());
            throw new GuiguException(201, "调用微信服务端失败");
        }

        // 登录时，先从 Redis 当中查，是否含有对应的登录的 token 值,有查 Redis 快，而不是直接就去访问MySQL
        String refreshTokenKey = RedisConstant.USER_LOGIN_REFRESH_KEY_PREFIX + openid;
        String jsonWebTokenFromRedis = redisTemplate.opsForValue().get(refreshTokenKey);
        if (!StringUtils.isEmpty(jsonWebTokenFromRedis)) {

            Map<String, Object> map1 = new HashMap<>();
            map1.put("token", jsonWebTokenFromRedis);
            // Redis 当获取到之后,直接返回不用走后面了
            return map1;
        }


        // 3. 调用 openId 查询用户信息
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getWxOpenId, openid);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

        if (userInfo == null) {
            // 1.像 user_info表中插入用户（注册用户信息）
            userInfo = new UserInfo();
            userInfo.setWxOpenId(openid);
            userInfo.setNickname(System.currentTimeMillis() + "rainbowsea" + UUID.randomUUID().toString().substring(0,
                    4).replace("-", ""));  // 昵称
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            userInfo.setIsVip(0);  // 不是vip
            userInfo.setVipExpireTime(new Date());
            int insert = userInfoMapper.insert(userInfo);
            log.info("注册用户：{}", insert > 0 ? "success" : "fail");

            // 向 tingshu_account 数据库中的 user_account表中插入用户账号信息(初始化用户账户余额)
            /**
             * param1: 交换机
             * param2: 路由键
             * param3: 消息内容
             */
            rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, userInfo.getId().toString());
            log.info("用户微服务发送初始化用户账户余额消息：{}成功", userInfo.getId());

        }


        Map<String, Object> map = new HashMap<>();
        // 传统方式
        //String token = UUID.randomUUID().toString().replace("-", "");
        //map.put("token",token);

        // 4. 生成一个 token 值返回给前端
        // 定义一个载荷(就是存放含有特别属性信息的 token 信息)
        String token = getJsonWebToken(openid, userInfo.getId());
        map.put("token", token); // 注意这 key 必须是 token 不可以是其他的，因为前端写死了,写其他的前端就获取不到这个token了
        map.put("refreshToken", token); // 假装有一个(因为前端只是做了一个 token 设计,没有做双 token)


        // 5. 将 token 存入到Redis 当中
        String accessTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openid;
        refreshTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openid;
        redisTemplate.opsForValue().set(accessTokenKey, token, 30, TimeUnit.MINUTES); // 30分钟
        redisTemplate.opsForValue().set(refreshTokenKey, token, 1, TimeUnit.DAYS);  // 一天
        return map;
    }

    /**
     * 生成一个载荷(含有我们自定义信息属性内容的载荷)的 token 值
     *
     * @param openid
     * @param userId
     * @return
     */
    private String getJsonWebToken(String openid, Long userId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", userId);
        jsonObject.put("openId", openid);

        /*
        RSA: 非对称加密的方式(公钥(加密和验签) 和 私钥(加签:防止数据被篡改,内部算法标记了)) ; 对称加密(一把钥匙)
        加签: 为了防止数据被篡改,一旦你加密的 token 其中的一部分被篡改了,都是无法通过验证的,因为它的token 是整体生成的
        加密: 将数据转成密文()
         */
        // jwt 方式生成
        Jwt jwt = JwtHelper.encode(jsonObject.toString(), rsaSigner);
        String encoded = jwt.getEncoded(); // 将 token 进行编码

        return encoded;
    }


    /**
     * 获取新的第二个 token 令牌的 token 值（双token 设计），前端没有实现
     *
     * @return
     */
    @Override
    public Map<String, Object> getNewAccessToken() {

        HashMap<String, Object> result = new HashMap<>();

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 1.2 获取请求对象
        HttpServletRequest request = requestAttributes.getRequest();
        // 1.3 获取请求对象的请求头
        String token = request.getHeader("token");
        if (StringUtils.isEmpty(token)) {
            throw new GuiguException(201, "之前没有登录过");
        }

        // 2.校验jsonWebToken是否被篡改了。
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(PublicConstant.PUBLIC_KEY));
        String claims = jwt.getClaims();
        Map map = JSONObject.parseObject(claims, Map.class);
        Object openId = map.get("openId");
        Object userId = map.get("id");


        String refreshTokenKey = RedisConstant.USER_LOGIN_REFRESH_KEY_PREFIX + openId;
        String accessTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openId;


        // 1.从Redis中获取RefreshToken
        String refreshToken = redisTemplate.opsForValue().get(refreshTokenKey);
        if (!StringUtils.isEmpty(refreshToken)) {
            String jsonWebToken = getJsonWebToken(String.valueOf(openId.toString()), Long.parseLong(userId.toString()));
            redisTemplate.opsForValue().set(accessTokenKey, jsonWebToken, 20, TimeUnit.DAYS);   //测试环境
            redisTemplate.opsForValue().set(refreshTokenKey, jsonWebToken, 180, TimeUnit.DAYS); // 长一点
            result.put("token", jsonWebToken);
            return result;

        } else {
            // 去登录
            result.put("1", "v");
        }

        return result;
    }

    @Override
    public void updateUser(UserInfoVo userInfoVo) {
        // 1.查询用户信息
        Long userId = AuthContextHolder.getUserId();

        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (null == userInfo) {
            throw new GuiguException(201, "用户信息不存在");
        }
        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        userInfoMapper.updateById(userInfo);
    }

    @Override
    public UserInfoVo getUserInfo(Long userId) {


        UserInfo userInfo = userInfoMapper.selectById(userId);

        if (userInfo == null) {
            throw new GuiguException(201, "用户不存在");
        }
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);

        return userInfoVo;
    }

    @Override
    public Map<Long, String> getUserPaidAlbumTrack(Long userId, Long albumId) {

        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getUserId, userId);
        wrapper.eq(UserPaidTrack::getAlbumId, albumId);
        List<UserPaidTrack> userPaidTracks = userPaidTrackMapper.selectList(wrapper);

        Map<Long, String> map = userPaidTracks.stream().collect(Collectors.toMap(UserPaidTrack::getTrackId, v -> "1"));
        return map;
    }

    @Override
    public Boolean getUserPaidAlbum(Long userId, Long albumId) {
        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getUserId, userId);
        wrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapper);
        return userPaidAlbum != null;
    }

    @Override
    public Boolean collect(Long trackId) {

        Long userId = AuthContextHolder.getUserId();

        // 1.构建条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("trackId").is(trackId);

        // 2.构建查询对象
        Query query = new Query(criteria);

        // 3.开始查询
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId);
        UserCollect userCollect = mongoTemplate.findOne(query, UserCollect.class, collectionName);
        if (userCollect == null) {
            // 收藏声音
            // 插入收藏对象到MongoDB
            userCollect = new UserCollect();
            userCollect.setId(ObjectId.get().toString());
            userCollect.setUserId(userId);
            userCollect.setTrackId(trackId);
            userCollect.setCreateTime(new Date());
            mongoTemplate.save(userCollect, collectionName);
            TrackStatMqVo trackStatMqVo = prepareTrackStatMqDto(null, userCollect.getTrackId(), SystemConstant.TRACK_STAT_COLLECT, 1);
            rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE, JSONObject.toJSONString(trackStatMqVo));

            return true;  // 收藏过
        } else {
            // 取消收藏
            mongoTemplate.remove(query, UserCollect.class, collectionName);
            TrackStatMqVo trackStatMqVo = prepareTrackStatMqDto(null, userCollect.getTrackId(), SystemConstant.TRACK_STAT_COLLECT, -1);
            rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE, JSONObject.toJSONString(trackStatMqVo));
            return false;

        }
    }

    @Override
    public Boolean isCollect(Long trackId) {

        Long userId = AuthContextHolder.getUserId();

        // 1.构建条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("trackId").is(trackId);

        // 2.构建查询对象
        Query query = new Query(criteria);

        long count = mongoTemplate.count(query, UserCollect.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        return count > 0;
    }

    @Override
    public Boolean isSubscribe(Long albumId) {


        Long userId = AuthContextHolder.getUserId();

        // 1.构建条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("albumId").is(albumId);

        // 2.构建查询对象
        Query query = new Query(criteria);

        long count = mongoTemplate.count(query, UserSubscribe.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId));
        return count > 0;
    }

    @Override
    public IPage<UserCollectVo> findUserCollectPage(IPage<UserCollectVo> pageParam) {

        Long userId = AuthContextHolder.getUserId();
        // 1.构建条件对象
        Criteria criteria = Criteria.where("userId").is(userId);

        // 2.构建查询对象
        Query query = new Query(criteria);

        // 3.构建分页和排序条件对象【mp中分页查询对MySQL查询有效】

        Sort sort = Sort.by(Sort.Order.desc("updateTime"));
        // from:(pn-1)*size   size
        PageRequest pageAndSort = PageRequest.of((int) ((pageParam.getCurrent() - 1) * pageParam.getSize()), (int) pageParam.getSize(), sort);
        query.with(pageAndSort);

        // 4.查询总记录数
        long count = mongoTemplate.count(query.limit(-1), UserCollect.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));


        // 5.查询数据
        List<UserCollect> userCollectList = mongoTemplate.find(query, UserCollect.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));

        List<Long> trackIdList = userCollectList.stream().map(UserCollect::getTrackId).collect(Collectors.toList());
        // 6.根据声音id集合 查询【声音对象】集合
        Result<List<TrackListVo>> trackListVoResult = albumInfoFeignClient.getTrackListByIds(trackIdList);
        List<TrackListVo> trackListVos = trackListVoResult.getData();
        if (CollectionUtils.isEmpty(trackListVos)) {
            throw new GuiguException(201, "远程查询专辑微服务获取声音集合失败");
        }

        //  7.将声音对象的列表集合转成声音对象的Map集合
        Map<Long, TrackListVo> trackListVoMap = trackListVos.stream().collect(Collectors.toMap(TrackListVo::getTrackId, v -> v));


        List<UserCollectVo> userCollectVoList = userCollectList.stream().map(userCollect -> {
            UserCollectVo userCollectVo = new UserCollectVo();
            TrackListVo trackListVo = trackListVoMap.get(userCollect.getTrackId());
            userCollectVo.setAlbumId(trackListVo.getAlbumId());// 收藏声音对应的专辑id
            userCollectVo.setTrackId(userCollect.getTrackId());
            userCollectVo.setCreateTime(userCollect.getCreateTime());
            userCollectVo.setTrackTitle(trackListVo.getTrackTitle());  // 收藏声音标题
            userCollectVo.setCoverUrl(trackListVo.getCoverUrl());  // 收藏声音的封面
            return userCollectVo;
        }).collect(Collectors.toList());
        return pageParam.setRecords(userCollectVoList).setTotal(count);
    }

}
