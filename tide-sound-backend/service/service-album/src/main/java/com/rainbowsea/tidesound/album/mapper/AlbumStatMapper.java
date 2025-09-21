package com.rainbowsea.tidesound.album.mapper;

import com.rainbowsea.tidesound.model.album.AlbumStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {

    /**
     * 更新专辑的播放量
     * @param albumId
     * @param albumStatPlay
     * @param count
     * @return
     */
    int updateAlbumNumByType(@Param("albumId") Long albumId, @Param("type") String albumStatPlay, @Param("num") Integer count);

}
