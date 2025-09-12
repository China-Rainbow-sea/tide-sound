package com.rainbowsea.tidesound.album.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rainbowsea.tidesound.album.service.AlbumInfoService;
import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.query.album.AlbumInfoQuery;
import com.rainbowsea.tidesound.vo.album.AlbumInfoVo;
import com.rainbowsea.tidesound.vo.album.AlbumListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;



    // Request URL: http://192.168.200.1:8500/api/album/albumInfo/findUserAllAlbumList
    @Operation(summary = "查询当前用户创做过的专辑列表")
    @GetMapping("/findUserAllAlbumList")
    @TingshuLogin
    public Result findUserAllAlbumList() {
        Long userId = AuthContextHolder.getUserId();
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList(userId);
        return Result.ok(albumInfoList);

    }


    // Request URL: http://192.168.200.1:8500/api/album/albumInfo/removeAlbumInfo/1596

    @Operation(summary = "根据专辑id删除专辑")
    @TingshuLogin
    @DeleteMapping("/removeAlbumInfo/{albumId}")
    public Result removeAlbumInfo(@PathVariable(value = "albumId") Long albumId) {

        albumInfoService.removeAlbumInfo(albumId);
        return Result.ok();
    }


    // Request URL: http://localhost:8500/api/album/albumInfo/updateAlbumInfo/1593
    @Operation(summary = "修改专辑信息")
    @TingshuLogin
    @PutMapping("/updateAlbumInfo/{albumId}")
    public Result updateAlbumInfo(@PathVariable(value = "albumId") Long albumId,
                                  @RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.updateAlbumInfo(albumId, albumInfoVo);
        return Result.ok();

    }




    // Request URL: http://192.168.200.1:8500/api/album/albumInfo/getAlbumInfo/1596

    @Operation(summary = "根据专辑id查询专辑信息")
    @TingshuLogin
    @GetMapping("/getAlbumInfo/{albumId}")
    public Result getAlbumInfo(@PathVariable(value = "albumId") Long albumId) {
        // 一定要返回AlbumInfo对象 不能返回AlbumInfoVo对象（专辑id）
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(albumId);
        return Result.ok(albumInfo);

    }


    // Request URL: http://192.168.200.1:8500/api/album/albumInfo/findUserAlbumPage/1/10
    @PostMapping("/findUserAlbumPage/{pn}/{pz}")
    @Operation(summary = "分页展示用户创作的专辑列表")
    @TingshuLogin
    public Result findUserAlbumPage(@PathVariable(value = "pn") Long pn,
                                    @PathVariable(value = "pz") Long pz,
                                    @RequestBody AlbumInfoQuery albumInfoQuery) {
        // 1.构建分页对象(对返回给前端的数据进行分页)
        IPage<AlbumListVo> pageParam = new Page<>(pn, pz);

        // 因为使用了 @TingshuLogin 登入认证注解,该注解将用户userId 封装到了LocalThread线程当中了，同一个线程处理,可以获取到
        albumInfoQuery.setUserId(AuthContextHolder.getUserId());
        // 2.将分页对象传进去(未来自动给Page对象的records属性赋值：list)
        pageParam = albumInfoService.findUserAlbumPage(pageParam, albumInfoQuery);

        // 3.将分页对象返回出去
        return Result.ok(pageParam);
    }




    // Request URL: http://localhost:8500/api/album/albumInfo/saveAlbumInfo
    // （接）RequestBody:将前端的json格式字符串 反序列化成javaBean对象
    // （返）@ResponseBody：将javaBean对象序列化成json格式的字符串

    // JSON:key  value:a={"name":"hzk","age":18}   User(name age)
    // json格式字符串："{"name":"hzk","age":18}"  普通文本格式字符串："abc" "123"

    // 注意：javaBean前面的@RequestBody注解只有当前端在请求体中提交的数据是json格式的字符串，才能加，反之如果只是在请求体中
    // 提交普通key,value格式，就不能加@RequestBody注解。

    @PostMapping("/saveAlbumInfo")
    @TingshuLogin
    @Operation(summary = "保存专辑信息")
    public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {

        albumInfoService.saveAlbumInfo(albumInfoVo);
        return Result.ok();
    }
}

