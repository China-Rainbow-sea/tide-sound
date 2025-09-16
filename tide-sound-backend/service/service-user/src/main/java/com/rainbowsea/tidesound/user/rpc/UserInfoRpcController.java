package com.rainbowsea.tidesound.user.rpc;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 用于微服务之间 RPC(同步)通信，处理业务
 */
@RestController
@RequestMapping("/api/inner/userinfo")
public class UserInfoRpcController {


    @Autowired
    private UserInfoService userInfoService;

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @GetMapping("/getUserInfo/{userId}")
    Result<UserInfoVo> getUserInfo(@PathVariable(value = "userId") Long userId) {
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }
}
