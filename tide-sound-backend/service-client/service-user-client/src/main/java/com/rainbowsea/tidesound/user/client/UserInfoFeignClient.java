package com.rainbowsea.tidesound.user.client;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.user.client.impl.UserInfoDegradeFeignClient;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

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


    /**
     * 获取用户付费专辑信息
     * @param userId
     * @param albumId
     * @return
     */
    @GetMapping("/getUserPaidAlbumTrack/{userId}/{albumId}")
    Result<Map<Long, String>> getUserPaidAlbumTrack(@PathVariable(value = "userId") Long userId,
                                                    @PathVariable(value = "albumId") Long albumId);


    /**
     * 获得用户付费专辑
     * @param userId
     * @param albumId
     * @return
     */
    @GetMapping("/getUserPaidAlbum/{userId}/{albumId}")
    Result<Boolean> getUserPaidAlbum(@PathVariable(value = "userId") Long userId,
                                     @PathVariable(value = "albumId") Long albumId);


    @GetMapping("/getVipConfigById/{itemId}")
    Result<VipServiceConfig> getVipConfigById(@PathVariable(value = "itemId") Long itemId);


    /**
     * 通过 openid 获取用户 id
     * @param openId
     * @return
     */
    @GetMapping("/getUserIdByOpenId/{openId}")
    Result<String> getUserIdByOpenId(@PathVariable(value = "openId") String openId);

}