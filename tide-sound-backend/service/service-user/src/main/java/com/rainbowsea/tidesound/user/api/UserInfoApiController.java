package com.rainbowsea.tidesound.user.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.result.ResultCodeEnum;
import com.rainbowsea.tidesound.user.service.UserInfoService;
import com.rainbowsea.tidesound.vo.user.UserCollectVo;
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



    // Request URL: http://192.168.200.1:8500/api/user/userInfo/findUserCollectPage/1/10
    @GetMapping("/findUserCollectPage/{pn}/{pz}")
    @Operation(summary = "查询用户收藏过的声音列表分页展示")
    @TingshuLogin
    public Result findUserCollectPage(@PathVariable(value = "pn") Long pn,
                                      @PathVariable(value = "pz") Long pz) {


        IPage<UserCollectVo> pageParam = new Page<UserCollectVo>(pn, pz);


        pageParam = userInfoService.findUserCollectPage(pageParam);
        return Result.ok(pageParam);
    }


    // 用户是否订阅过专辑
    // Request URL: http://192.168.200.1:8500/api/user/userInfo/isSubscribe/1598
    @GetMapping("/isSubscribe/{albumId}")
    @Operation(summary = "是否订阅过专辑")
    @TingshuLogin
    public Result isSubscribe(@PathVariable(value = "albumId") Long albumId) {

        Boolean flag = userInfoService.isSubscribe(albumId);
        return Result.ok(flag);
    }

    // Request URL: http://192.168.200.1:8500/api/user/userInfo/isCollect/51945
    @GetMapping("/isCollect/{trackId}")
    @Operation(summary = "是否收藏声音")
    @TingshuLogin
    public Result isCollect(@PathVariable(value = "trackId") Long trackId) {

        Boolean flag = userInfoService.isCollect(trackId);
        return Result.ok(flag);

    }

    // Request URL: http://192.168.200.1:8500/api/user/userInfo/collect/36362
    @GetMapping("/collect/{trackId}")
    @TingshuLogin
    @Operation(summary = "收藏与取消收藏声音")
    public Result collect(@PathVariable(value = "trackId") Long trackId) {
        Boolean flag = userInfoService.collect(trackId);
        return Result.ok(flag);
    }



    // 前端请求: http://192.168.76.15:8500/api/user/userInfo/findUserSubscribePage/1/10
    @Operation(summary = "模拟登录弹窗提示")
    @GetMapping("/findUserSubscribePage/{pn}/{pz}")
    public Result findUserSubscribePage(@PathVariable(value = "pn") Long pn,
                                        @PathVariable(value = "pz") Long pz) {

        //return Result.build(null, ResultCodeEnum.LOGIN_AUTH);
        return Result.ok();
    }

}

