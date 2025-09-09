package com.rainbowsea.tidesound.common.login.aspect;


import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.common.constant.PublicConstant;
import com.rainbowsea.tidesound.common.constant.RedisConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.ResultCodeEnum;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 获取登录信息，认证切面
 */
@Aspect
@Component
public class LoginAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;



    //  多切面的失效问题
    //  1.登录切面(业务)
    //  2.事务切面（非业务）
    @Around(value = "@annotation(com.rainbowsea.tidesound.common.login.annotation.TingshuLogin)")
    public Object loginCheck(ProceedingJoinPoint pjp) throws Throwable {


        // 1.获取请求中的令牌
        String jsonWebToken = getJsonWebToken();

        // 3. 判断是否携带了令牌（如果携带令牌还将载荷中的数据获取到）
        Long userId = checkTokenAndGetUserId(jsonWebToken);

        // 4. 将认证中心获取到的 userId放到 ThreadLocal中，同一个线程存储数据
        AuthContextHolder.setUserId(userId);
//        ThreadLocal<Long> longThreadLocal = new ThreadLocal<>();
//        longThreadLocal.set(userId);

        Object retVal;
        try {
            retVal = pjp.proceed(); //  执行目标方法
        } finally {
            AuthContextHolder.removeUserId();  // 解决内存泄漏
        }

        // 5. 返回结果
        return retVal;
    }


    /**
     * 1.验证前端发送过来的这个 token 是否有效，是否被篡改,是否过期
     * 2.同时对 token 进行解码，获取其中当时登录时，存放在 token 当中的载荷信息(属性值)
     * 3.同时比对 Redis 当中存放的 token值是否一致,是否被篡改
     * @param jsonWebTokenFromWeb
     * @return
     */
    private Long checkTokenAndGetUserId(String jsonWebTokenFromWeb) {

        // 1.校验是否该请求中携带了
        if (StringUtils.isEmpty(jsonWebTokenFromWeb)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 2.通过公钥 PublicConstant.PUBLIC_KEY(自定义的常量) , 校验 jsonWebToken是否被篡改了。
        Jwt jwt = JwtHelper.decodeAndVerify(jsonWebTokenFromWeb, new RsaVerifier(PublicConstant.PUBLIC_KEY));

        // 3.校验通过 获取载荷数据(注意:登录成功时,怎么设置的token信息,就怎么获取到解码token当中的信息“设置-解的 key 必须是一致的”)
        String claims = jwt.getClaims();
        Map map = JSONObject.parseObject(claims, Map.class);
        Object userId = map.get("id");
        Object openId = map.get("openId");


        // 4.比 对 Redis 中是否存在 jsonWebToken
        String accessTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openId;
        String accessTokenFromRedis = redisTemplate.opsForValue().get(accessTokenKey);
        if (StringUtils.isEmpty(accessTokenFromRedis) || !jsonWebTokenFromWeb.equals(accessTokenFromRedis)) {
            throw new GuiguException(401, "accessToken已过期");
        }


        return Long.parseLong(userId.toString());


    }


    /**
     * 获取请求头当中的 token 值
     * @return
     */
    private static String getJsonWebToken() {
        // 1.获取用户的身份信息
        // 1.1获取目标请求属性对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 1.2 获取请求对象
        HttpServletRequest request = requestAttributes.getRequest();
        // 1.3 获取请求对象的请求头
        String token = request.getHeader("token");
        return token;
    }
}
