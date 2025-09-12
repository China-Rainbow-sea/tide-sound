package com.rainbowsea.tidesound.album.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.query.album.TrackInfoQuery;
import com.rainbowsea.tidesound.vo.album.TrackInfoVo;
import com.rainbowsea.tidesound.vo.album.TrackListVo;
import org.springframework.web.multipart.MultipartFile;

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
}
