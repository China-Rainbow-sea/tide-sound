package com.rainbowsea.tidesound.album.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rainbowsea.tidesound.query.album.AlbumInfoQuery;
import com.rainbowsea.tidesound.vo.album.AlbumListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {


    /**
     * 分页展示用户创作的专辑列表
     *
     * @param pageParam
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> findUserAlbumPage(@Param("pageParam") IPage<AlbumListVo> pageParam, @Param("vo") AlbumInfoQuery albumInfoQuery);


}
