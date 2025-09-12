package com.rainbowsea.tidesound.album.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rainbowsea.tidesound.query.album.TrackInfoQuery;
import com.rainbowsea.tidesound.vo.album.TrackListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {

    /**
     * 分页展示用户创作的声音列表
     *
     * @param pageParam
     * @param trackInfoQuery
     * @return
     */

    IPage<TrackListVo> findUserTrackPage(@Param("pageParam") IPage<TrackListVo> pageParam, @Param("vo") TrackInfoQuery trackInfoQuery);

}
