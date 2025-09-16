package com.rainbowsea.tidesound.album.client.impl;


import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.rainbowsea.tidesound.vo.album.AlbumStatVo;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Component
public class AlbumInfoDegradeFeignClient implements AlbumInfoFeignClient {


    /**
     * 根据专辑id获取基本该专辑 3级类别列表
     * @param c1Id
     * @return
     */
    @Override
    public Result<List<BaseCategory3>> getBaseCategory3ListByC1Id(Long c1Id) {
        // TODO 降级逻辑
        return Result.fail();
    }





    /**
     * 获取通过专辑id 获取到专辑的状态信息
     * @param albumId
     * @return
     */
    @Override
    public Result<AlbumStatVo> getAlbumStat(Long albumId) {
        // TODO 降级逻辑
        return Result.fail();
    }

    /**
     * 通过专辑 id 获取到专辑类别信息
     * @param albumId
     * @return
     */
    @Override
    public Result<BaseCategoryView> getAlbumCategory(Long albumId) {
        // TODO 降级逻辑
        return Result.fail();
    }

    /**
     * 通过专辑id 获取到专辑属性信息
     * @param albumId
     * @return
     */
    @Override
    public Result<AlbumInfo> getAlbumInfoAndAttrValue(Long albumId) {
        // TODO 降级逻辑
        return Result.fail();
    }

}
