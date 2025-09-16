package com.rainbowsea.tidesound.user.client;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.user.client.impl.UserInfoDegradeFeignClient;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-user", fallback = UserInfoDegradeFeignClient.class
,path = "/api/inner/userinfo")
public interface UserInfoFeignClient {


    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @GetMapping("/getUserInfo/{userId}")
    Result<UserInfoVo> getUserInfo(@PathVariable(value = "userId") Long userId);

}