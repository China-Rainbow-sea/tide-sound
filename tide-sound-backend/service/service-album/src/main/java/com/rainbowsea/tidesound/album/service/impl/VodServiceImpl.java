package com.rainbowsea.tidesound.album.service.impl;

import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.rainbowsea.tidesound.album.config.VodProperties;
import com.rainbowsea.tidesound.album.service.VodService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.util.UploadFileUtil;
import com.rainbowsea.tidesound.vo.album.TrackMediaInfoVo;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.DeleteMediaRequest;
import com.tencentcloudapi.vod.v20180717.models.DeleteMediaResponse;
import com.tencentcloudapi.vod.v20180717.models.DescribeMediaInfosRequest;
import com.tencentcloudapi.vod.v20180717.models.DescribeMediaInfosResponse;
import com.tencentcloudapi.vod.v20180717.models.MediaBasicInfo;
import com.tencentcloudapi.vod.v20180717.models.MediaInfo;
import com.tencentcloudapi.vod.v20180717.models.MediaMetaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class VodServiceImpl implements VodService {

    @Autowired
    private VodProperties vodProperties;


    /**
     * 上传音频文件到 腾讯云-云点播-vod中
     * @param file
     * @return
     */
    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {


        // 1.构建vod的客户端对象
        VodUploadClient vodClient = new VodUploadClient(vodProperties.getSecretId(), vodProperties.getSecretKey());


        // 过渡获取到文件在磁盘上的完整路径（腾讯云-云点播-无法直接将我们的文件没有路径传输到云点播，需要在我们的项目当中创建一个）
        // 临时文件，再将存储到我们项目的临时文件当中的音频文件，上传到腾讯云-云点播当中
        String mediaFileUrl = UploadFileUtil.uploadTempPath(vodProperties.getTempPath(), file);

        // 2.构建上传请求对象
        VodUploadRequest request = new VodUploadRequest();
        request.setMediaFilePath(mediaFileUrl);
        request.setSubAppId(Long.parseLong(vodProperties.getAppId().toString()));  // 设置指定是操作那个: appID云点播的应用
        request.setStorageRegion(vodProperties.getRegion());
        request.setConcurrentUploadNumber(5); // 5个线程并发上传

        // 3.开始上传
        try {
            VodUploadResponse response = vodClient.upload(vodProperties.getRegion(), request);
            log.info("Upload FileId = {}", response.getFileId());

            // 参数是固定的不可以修改
            HashMap<String, Object> map = new HashMap<>();
            map.put("mediaFileId", response.getFileId());
            map.put("mediaUrl", response.getMediaUrl());
            // 4. 上传成功(和失败)应该删除保存到本项目的临时文件下的音频内容 路径: vodProperties.getTempPath()
            return map;
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error("上传音频文件到vod中失败：{}", e.getMessage());
            throw new GuiguException(201, "上传音频文件到vod失败");
        }

    }



    /**
     * 根据媒体文件 id  获取媒体信息
     * 通过腾讯云-云点播，返回的该音频文件的 ID 信息，
     * 获取到其云点播当中存储该音频文件的信息(文件类型，文件大小，文件时长等等)
     *
     * @param mediaFileId
     * @return
     */
    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {

        try {

            // 1.创建实例认证对象
            Credential cred = new Credential(vodProperties.getSecretId(), vodProperties.getSecretKey());

            // 2.创建查询媒体信息的vod客户端对象
            VodClient client = new VodClient(cred, vodProperties.getRegion());

            // 3.创建Request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            req.setSubAppId(Long.parseLong(vodProperties.getAppId().toString()));  // 设置指定是操作那个: appID云点播的应用
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);
            // 4.发起请求 并且得到响应对象
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);
            // 输出json格式的字符串回包
           log.info("云点播返回的存储音频文件信息: {}" ,AbstractModel.toJsonString(resp));

            MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
            if (mediaInfoSet != null && mediaInfoSet.length != 0) {
                MediaInfo mediaInfo = mediaInfoSet[0];

                MediaBasicInfo basicInfo = mediaInfo.getBasicInfo();
                MediaMetaData metaData = mediaInfo.getMetaData();

                TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
                trackMediaInfoVo.setSize(metaData.getSize());
                trackMediaInfoVo.setDuration(metaData.getAudioDuration());
                trackMediaInfoVo.setMediaUrl(basicInfo.getMediaUrl());
                trackMediaInfoVo.setType(basicInfo.getType());
                return trackMediaInfoVo;
            }


        } catch (TencentCloudSDKException e) {
            log.error("查询媒体信息失败：{}", e.getMessage());
        }


        return null;
    }


    /**
     *  根据媒体文件id  删除媒体信息
     * @param mediaFileId
     */
    @Override
    public void removeMediaFile(String mediaFileId) {
        try {
            // 1.创建实例认证对象
            Credential cred = new Credential(vodProperties.getSecretId(), vodProperties.getSecretKey());

            // 2.创建删除媒体信息的vod客户端对象
            VodClient client = new VodClient(cred, vodProperties.getRegion());
            // 3.创建删除对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setSubAppId(Long.parseLong(vodProperties.getAppId().toString()));  // 设置指定是操作那个: appID云点播的应用
            req.setFileId(mediaFileId);
            // 4.发起请求 并且得到响应对象
            DeleteMediaResponse resp = client.DeleteMedia(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("删除媒体信息失败：{}", e.getMessage());
            throw new GuiguException(201, "删除媒体信息失败");
        }
    }

}
