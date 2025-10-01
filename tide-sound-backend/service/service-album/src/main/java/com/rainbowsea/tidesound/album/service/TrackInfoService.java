package com.rainbowsea.tidesound.album.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.query.album.TrackInfoQuery;
import com.rainbowsea.tidesound.vo.album.AlbumTrackListVo;
import com.rainbowsea.tidesound.vo.album.TrackInfoVo;
import com.rainbowsea.tidesound.vo.album.TrackListVo;
import com.rainbowsea.tidesound.vo.album.TrackStatVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {
    /**
     * 通过腾讯云-云点播-上传声音
     *
     * @param file
     * @return
     */
    Map<String, Object> uploadTrack(MultipartFile file);


    /**
     * 保存声音到对应的专辑当中去
     * @param trackInfoVo
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo);

    /**
     * 分页展示用户创作的声音列表
     *
     * @param pageParam
     * @param trackInfoQuery
     * @return
     */
    IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageParam, TrackInfoQuery trackInfoQuery);

    /**
     * 实现修改声音专栏的信息
     * @param trackId
     * @param trackInfoVo
     */
    void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo);

    /**
     * 根据声音id删除声音
     *
     * @param trackId
     */
    void removeTrackInfo(Long trackId);

    /**
     * 根据专辑 id 查询专辑下声音列表且显示付费图标
     * @param albumTrackListVoPage
     * @param albumId
     * @return
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> albumTrackListVoPage, Long albumId);

    /**
     * 根据声音id查询声音的统计信息
     *
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(Long trackId);

    /**
     * 根据专辑id查询专辑信息
     *
     * @param albumId
     * @return
     */
    AlbumInfo getAlbumInfo(Long albumId);

    /**
     * 根据声音id集合查询声音对象集合
     *
     * @param trackIdList
     * @return
     */
    List<TrackListVo> getTrackListByIds(List<Long> trackIdList);

    /**
     * 分集展示要买的声音列表
     *
     * @param currentTrackId
     * @return
     */
    List<Map<String, Object>> findUserTrackPaidList(Long currentTrackId);

    /**
     * 查询当前声音后多少集声音列表
     *
     * @param userId
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> getTrackListByCurrentTrackId(Long userId, Long trackId, Integer trackCount);

}
