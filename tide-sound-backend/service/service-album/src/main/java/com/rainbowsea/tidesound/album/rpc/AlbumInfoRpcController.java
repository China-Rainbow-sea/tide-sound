package com.rainbowsea.tidesound.album.rpc;

import com.rainbowsea.tidesound.album.service.AlbumInfoService;
import com.rainbowsea.tidesound.album.service.BaseCategoryService;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.rainbowsea.tidesound.vo.album.AlbumStatVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 用于其他微服务使用 OpenFegin 调用处理业务，微服务之间的 RPC (同步)通信
 */
@RestController
@RequestMapping("/rpc/inner/albuminfo")
public class AlbumInfoRpcController {


    @Autowired
    private AlbumInfoService albumInfoService;


    @Autowired
    private BaseCategoryService baseCategoryService;


    /**
     * 查询全平台的一级分类 id
     * @return
     */
    @GetMapping("/getAllCategory1Id")
    Result<List<Long>> getAllCategory1Id() {

        List<Long> c1Ids = baseCategoryService.getAllCategory1Id();
        return Result.ok(c1Ids);
    }


    /**
     * 根据专辑id获取基本该专辑 3级类别列表
     * @param c1Id
     * @return
     */
    @GetMapping("/getBaseCategory3ListByC1Id/{albumId}")
    Result<List<BaseCategory3>> getBaseCategory3ListByC1Id(@PathVariable(value = "albumId") Long c1Id) {
        List<BaseCategory3> topBaseCategory3 = baseCategoryService.findTopBaseCategory3(c1Id);
        return Result.ok(topBaseCategory3);
    }



    /**
     * 获取通过专辑id 获取到专辑的状态信息
     * @param albumId
     * @return
     */
    @GetMapping("/getAlbumStat/{albumId}")
    Result<AlbumStatVo> getAlbumStat(@PathVariable(value = "albumId") Long albumId) {

        AlbumStatVo albumStatVo = albumInfoService.getAlbumStat(albumId);
        return Result.ok(albumStatVo);
    }


    /**
     * 获取专辑类别
     * @param albumId
     * @return
     */
    @GetMapping("/getAlbumCategory/{albumId}")
    Result<BaseCategoryView> getAlbumCategory(@PathVariable(value = "albumId") Long albumId) {

        BaseCategoryView baseCategoryView = baseCategoryService.getAlbumCategory(albumId);
        return Result.ok(baseCategoryView);

    }

    /**
     * 获取专辑的信息和属性值
     * @param albumId
     * @return
     */
    @GetMapping("/getAlbumInfoAndAttrValue/{albumId}")
    Result<AlbumInfo> getAlbumInfoAndAttrValue(@PathVariable(value = "albumId") Long albumId){

        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(albumId);
        return Result.ok(albumInfo);
    }
}