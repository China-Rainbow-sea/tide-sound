package com.rainbowsea.tidesound.user.client.impl;


import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import org.springframework.stereotype.Component;

@Component
public class UserInfoDegradeFeignClient implements UserInfoFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfo(Long userId) {
        // TODO 降级逻辑
        return Result.fail();
    }
}
