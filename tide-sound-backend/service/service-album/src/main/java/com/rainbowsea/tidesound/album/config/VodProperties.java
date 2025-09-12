package com.rainbowsea.tidesound.album.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vod") //读取节点
@Data
public class VodProperties {

    private String appId;
    private String secretId;
    private String secretKey;
    private String region;
    private String tempPath;

}