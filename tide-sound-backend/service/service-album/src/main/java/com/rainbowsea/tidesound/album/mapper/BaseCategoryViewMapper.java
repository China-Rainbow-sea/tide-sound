package com.rainbowsea.tidesound.album.mapper;

import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BaseCategoryViewMapper extends BaseMapper<BaseCategoryView> {

    /**
     * 获取专辑类别
     * @param albumId
     * @return
     */
    BaseCategoryView getAlbumCategory(@Param("albumId") Long albumId);
}
