package com.rainbowsea.tidesound.album.client;

import com.rainbowsea.tidesound.album.client.impl.AlbumInfoDegradeFeignClient;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.rainbowsea.tidesound.vo.album.AlbumStatVo;
import com.rainbowsea.tidesound.vo.album.TrackListVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = AlbumInfoDegradeFeignClient.class
,contextId = "albumInfoFeignClient",path = "/rpc/inner/albuminfo")
public interface AlbumInfoFeignClient {


    /**
     * 获取通过专辑id 获取到专辑的状态信息
     * @param albumId
     * @return
     */
    @GetMapping("/getAlbumStat/{albumId}")
    Result<AlbumStatVo> getAlbumStat(@PathVariable(value = "albumId") Long albumId);



    /**
     * 获取专辑类别
     * @param albumId
     * @return
     */
    @GetMapping("/getAlbumCategory/{albumId}")
    Result<BaseCategoryView> getAlbumCategory(@PathVariable(value = "albumId") Long albumId);



    /**
     * OpenFeign 去微服务注册到 Nacos中心的service-album微服务/rpc/inner/albuminfo/getAlbumInfoAndAttrValue/路径
     * 发送请求。的获取相册信息和属性值
     * @param albumId
     * @return
     */
    @GetMapping("/getAlbumInfoAndAttrValue/{albumId}")
    Result<AlbumInfo> getAlbumInfoAndAttrValue(@PathVariable(value = "albumId") Long albumId);


    /**
     * 根据专辑id获取基本该专辑 3级类别列表
     * @param c1Id
     * @return
     */
    @GetMapping("/getBaseCategory3ListByC1Id/{albumId}")
    Result<List<BaseCategory3>> getBaseCategory3ListByC1Id(@PathVariable(value = "albumId") Long c1Id);

    /**
     * 查询全平台的一级分类 id
     * @return
     */
    @GetMapping("/getAllCategory1Id")
    Result<List<Long>> getAllCategory1Id();


    /**
     * 按id获取专辑声音列表
     * @param trackIdList
     * @return
     */
    @PostMapping("/getTrackListByIds")
    Result<List<TrackListVo>> getTrackListByIds(@RequestBody List<Long> trackIdList);


}