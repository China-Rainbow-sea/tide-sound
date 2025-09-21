package com.rainbowsea.tidesound.album.service.impl;


import com.rainbowsea.tidesound.album.mapper.AlbumStatMapper;
import com.rainbowsea.tidesound.album.mapper.TrackStatMapper;
import com.rainbowsea.tidesound.album.service.MqOpsService;
import com.rainbowsea.tidesound.common.constant.SystemConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.vo.album.TrackStatMqVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private TrackStatMapper trackStatMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void trackStatTypeUpdate(TrackStatMqVo trackStatMqVo) {

        // 1.获取参数
        Long trackId = trackStatMqVo.getTrackId();
        Long albumId = trackStatMqVo.getAlbumId();
        String statType = trackStatMqVo.getStatType();
        Integer count = trackStatMqVo.getCount();

        try {

            if (albumId != null) {
                // 2.更新专辑的播放量（没有收藏量）
                int affectAlbumUpdateRows = albumStatMapper.updateAlbumNumByType(albumId, SystemConstant.ALBUM_STAT_PLAY, count);
                log.info("更新专辑的播放量：{}", affectAlbumUpdateRows > 0 ? "成功" : "失败");
            }
            // 3.更新声音的播放量/收藏量
            int affectTrackUpdateRows = trackStatMapper.updateTrackNumByType(trackId, statType, count);
            log.info("更新声音的播放量：{}", affectTrackUpdateRows > 0 ? "成功" : "失败");

        } catch (Exception e) {
            throw new GuiguException(201, "更新声音的播放量失败");
        }
    }
}
