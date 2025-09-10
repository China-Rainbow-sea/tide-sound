package com.rainbowsea.tidesound.user.service;

import com.rainbowsea.tidesound.model.user.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 微信小程序登录(微信登录)
     * @param code
     * @return
     */
    Map<String, Object> wxLogin(String code);

    /**
     * 向 Redis 获取第二个refreshToken 的 token 值(双token 的设计的情况下)
     * @return
     */
    Map<String, Object> getNewAccessToken();


    /**
     * 更新用户信息：用户头像，用户昵称
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);
}
