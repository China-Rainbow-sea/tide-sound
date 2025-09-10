package com.rainbowsea.tidesound.common.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

//@Configuration
@ConfigurationProperties(prefix="minio") //读取节点
@Data
public class MinioProperties {

    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
