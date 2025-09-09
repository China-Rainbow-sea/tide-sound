package com.rainbowsea.tidesound.user.api;

import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.result.ResultCodeEnum;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.user.UserInfo;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    @GetMapping("/wxLogin/{code}")
    @Operation(summary = "微信小程序登录")
    public Result wexLogin(@PathVariable(value = "code") String code) {
        // 左边（前端要的）== 右边(实现的功能)
        Map<String,Object> map = userInfoService.wxLogin(code);

        // 登录成功返回给前端的map,包含着后端给前端的认证的 token 信息
        return Result.ok(map);
    }

    // Request URL: http://192.168.200.1:8500/api/user/wxLogin/getUserInfo
    @GetMapping("/getUserInfo")
    @Operation(summary = "获取用户信息")
    @TingshuLogin // 登录认证
    public Result getUserInfo() {

        // 从ThreadLocal线程当中获取存储了的 userId
        Long userId = AuthContextHolder.getUserId();

        UserInfo userInfo = userInfoService.getById(userId);
        if(userInfo == null) {
            throw new GuiguException(201,"该用户不存在");
        }

        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo,userInfoVo);
        return Result.ok(userInfoVo);
    }


    @GetMapping("/refreshToken/getNewAccessToken")
    @Operation(summary = "获取新的令牌(双 token 设计的情况下)")
    @TingshuLogin
    public Result getNewAccessToken() {
        Map<String, Object> map = userInfoService.getNewAccessToken();
        Object flag = map.get("1");
        // 含有 1 的 value 值说明，用户从来就没有登录过，让其登录去
        if(map != null && StringUtils.isEmpty(flag)) {
            return Result.build(null, ResultCodeEnum.LOGIN_AUTH);
        }
        return Result.ok(map);
    }

}
