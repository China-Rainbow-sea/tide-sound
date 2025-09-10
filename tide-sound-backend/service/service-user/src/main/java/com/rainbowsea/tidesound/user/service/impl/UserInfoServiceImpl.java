package com.rainbowsea.tidesound.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.common.constant.PublicConstant;
import com.rainbowsea.tidesound.common.constant.RedisConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.rabbit.service.RabbitService;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.user.UserInfo;
import com.rainbowsea.tidesound.user.mapper.UserInfoMapper;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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


    /**
     * 返回微信登录成功后的 Map ,Map 当中存放了JWT认证的 token 信息
     * @param code
     * @return
     */
    @Override
    public Map<String, Object> wxLogin(String code) {
        // 1. 判断 code 码是否存在
        if(StringUtils.isEmpty(code)) {
            throw new GuiguException(201,"code 不存在");
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
        queryWrapper.eq(UserInfo::getWxOpenId,openid);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

        if(userInfo == null) {
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
        map.put("token",token) ; // 注意这 key 必须是 token 不可以是其他的，因为前端写死了,写其他的前端就获取不到这个token了
        map.put("refreshToken",token); // 假装有一个(因为前端只是做了一个 token 设计,没有做双 token)


        // 5. 将 token 存入到Redis 当中
        String accessTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openid;
        refreshTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openid;
        redisTemplate.opsForValue().set(accessTokenKey,token,30, TimeUnit.MINUTES); // 30分钟
        redisTemplate.opsForValue().set(refreshTokenKey,token,1,TimeUnit.DAYS);  // 一天
        return map;
    }

    /**
     * 生成一个载荷(含有我们自定义信息属性内容的载荷)的 token 值
     * @param openid
     * @param userId
     * @return
     */
    private String getJsonWebToken(String openid,Long userId) {
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

}
