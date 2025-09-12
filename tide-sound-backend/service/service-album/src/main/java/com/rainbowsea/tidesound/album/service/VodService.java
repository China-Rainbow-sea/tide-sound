package com.rainbowsea.tidesound.album.service;

import com.rainbowsea.tidesound.vo.album.TrackMediaInfoVo;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {


    /**
     * 上传音频文件到 腾讯云-云点播-vod中
     *
     * @param file
     * @return
     */
    Map<String, Object> uploadTrack(MultipartFile file);

    /**
     * 根据媒体文件 id  获取媒体信息
     * 通过腾讯云-云点播，返回的该音频文件的 ID 信息，
     * 获取到其云点播当中存储该音频文件的信息(文件类型，文件大小，文件时长等等)
     *
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    /**
     *  根据媒体文件id  删除媒体信息
     * @param mediaFileId
     */
    void removeMediaFile(String mediaFileId);
}
