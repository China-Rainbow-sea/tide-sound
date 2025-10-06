package com.rainbowsea.tidesound.user.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.user.UserInfo;
import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import com.rainbowsea.tidesound.user.service.VipServiceConfigService;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * 用于微服务之间 RPC(同步)通信，处理业务
 */
@RestController
@RequestMapping("/api/inner/userinfo")
public class UserInfoRpcController {


    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private VipServiceConfigService vipServiceConfigService;

    /**
     * 获取用户信息
     *
     * @param userId
     * @return
     */
    @GetMapping("/getUserInfo/{userId}")
    Result<UserInfoVo> getUserInfo(@PathVariable(value = "userId") Long userId) {
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }


    /**
     * 查询用户买过当前专辑下的声音
     *
     * @param userId
     * @param albumId
     * @return
     */
    @GetMapping("/getUserPaidAlbumTrack/{userId}/{albumId}")
    Result<Map<Long, String>> getUserPaidAlbumTrack(@PathVariable(value = "userId") Long userId,
                                                    @PathVariable(value = "albumId") Long albumId) {
        Map<Long, String> map = userInfoService.getUserPaidAlbumTrack(userId, albumId);
        return Result.ok(map);
    }

    /**
     * 查询用户买过该专辑
     * @param userId
     * @param albumId
     * @return
     */
    @GetMapping("/getUserPaidAlbum/{userId}/{albumId}")
    Result<Boolean> getUserPaidAlbum(@PathVariable(value = "userId") Long userId,
                                     @PathVariable(value = "albumId") Long albumId) {

        Boolean isPaidAlbum = userInfoService.getUserPaidAlbum(userId, albumId);
        return Result.ok(isPaidAlbum);
    }


    /**
     * 根据用户 id ,查询
     * @param itemId
     * @return
     */
    @GetMapping("/getVipConfigById/{itemId}")
    Result<VipServiceConfig> getVipConfigById(@PathVariable(value = "itemId") Long itemId) {

        VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(itemId);

        return Result.ok(vipServiceConfig);


    }


    /**
     * 通过 openid 获取用户 id
     * @param openId
     * @return
     */
    @GetMapping("/getUserIdByOpenId/{openId}")
    Result<String> getUserIdByOpenId(@PathVariable(value = "openId") String openId) {


        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>();
        wrapper.eq(UserInfo::getWxOpenId, openId);
        UserInfo userInfo = userInfoService.getOne(wrapper);


        return Result.ok(userInfo.getId().toString());
    }

}
