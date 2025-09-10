package com.rainbowsea.tidesound.common.minio.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * 图片文件的上传
     * @param file
     * @return
     */
    String fileUploadService(MultipartFile file);
}
