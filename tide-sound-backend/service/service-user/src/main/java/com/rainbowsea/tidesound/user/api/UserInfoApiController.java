package com.rainbowsea.tidesound.user.api;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.result.ResultCodeEnum;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.service.OperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private OperationService operationBuilder;

    // 前端请求: http://192.168.76.15:8500/api/user/userInfo/findUserSubscribePage/1/10
    @Operation(summary = "模拟登录弹窗提示")
    @GetMapping("/findUserSubscribePage/{pn}/{pz}")
    public Result findUserSubscribePage(@PathVariable(value = "pn") Long pn,
                                        @PathVariable(value = "pz") Long pz) {

        return Result.build(null, ResultCodeEnum.LOGIN_AUTH);
//        return Result.ok();
    }

}

