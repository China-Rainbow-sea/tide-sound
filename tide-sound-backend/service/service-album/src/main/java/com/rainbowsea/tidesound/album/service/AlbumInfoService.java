package com.rainbowsea.tidesound.album.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.query.album.AlbumInfoQuery;
import com.rainbowsea.tidesound.vo.album.AlbumInfoVo;
import com.rainbowsea.tidesound.vo.album.AlbumListVo;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /**
     * 保存专辑信息
     * @param albumInfoVo
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo);


    /**
     * 分页展示用户创作的专辑列表
     * @param pageParam
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> pageParam, AlbumInfoQuery albumInfoQuery);



    /**
     * 根据专辑id查询专辑信息
     *
     * @param albumId
     * @return
     */
    AlbumInfo getAlbumInfo(Long albumId);


    /**
     * 修改专辑信息
     * @param albumId
     * @param albumInfoVo
     */
    void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo);
    /**
     * 根据专辑id删除专辑
     *
     * @param albumId
     */
    void removeAlbumInfo(Long albumId);

    /**
     * 查询当前用户创做过的专辑列表
     *
     * @param userId
     * @return
     */
    List<AlbumInfo> findUserAllAlbumList(Long userId);

}
