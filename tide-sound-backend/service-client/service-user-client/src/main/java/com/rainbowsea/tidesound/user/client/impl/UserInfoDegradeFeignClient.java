package com.rainbowsea.tidesound.user.client.impl;


import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserInfoDegradeFeignClient implements UserInfoFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfo(Long userId) {
        // TODO 降级逻辑
        return Result.fail();
    }


    @Override
    public Result<Map<Long, String>> getUserPaidAlbumTrack(Long userId, Long albumId) {
        // TODO 降级逻辑
        return Result.fail();
    }

    @Override
    public Result<Boolean> getUserPaidAlbum(Long userId, Long albumId) {
        // TODO 降级逻辑
        return Result.fail();
    }

    @Override
    public Result<VipServiceConfig> getVipConfigById(Long itemId) {
        // TODO 降级逻辑
        return Result.fail();
    }
}
