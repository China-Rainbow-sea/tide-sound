package com.rainbowsea.tidesound.search.service;

import com.rainbowsea.tidesound.vo.search.AlbumInfoIndexVo;

import java.util.List;
import java.util.Map;

public interface ItemService {


    /**
     * 专辑的上架
     *
     * @param albumId
     */
    void albumOnSale(Long albumId);

    /**
     * 专辑的下架
     *
     * @param albumId
     */
    void albumOffSale(Long albumId);


    /**
     * 专辑批量的下架
     */
    void batchAlbumOffSale();

    /**
     * 根据专辑id查询专辑详情
     *
     * @param albumId
     * @return
     */
    Map<String, Object> getAlbumInfo(Long albumId);

    /**
     * 提前将Es中的排行榜数据缓存到Redis中
     */
    void preRankingToCache();

    /**
     * 查询排行榜
     *
     * @param c1Id
     * @param dimension
     * @return
     */
    List<AlbumInfoIndexVo> findRankingList(Long c1Id, String dimension);
}
