package com.rainbowsea.tidesound.album.mapper;

import com.rainbowsea.tidesound.model.album.TrackStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rainbowsea.tidesound.vo.album.TrackStatVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackStatMapper extends BaseMapper<TrackStat> {


    /**
     * 根据声音id 查询声音统计信息
     *
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(@Param("trackId") Long trackId);


    /**
     * 更新声音统计信息
     *
     * @param trackId
     * @param statType
     * @param count
     * @return
     */
    int updateTrackNumByType(@Param("trackId") Long trackId, @Param("type") String statType, @Param("num") Integer count);


}
