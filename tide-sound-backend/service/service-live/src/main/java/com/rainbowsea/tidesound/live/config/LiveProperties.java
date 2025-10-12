package com.rainbowsea.tidesound.live.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "live")
@Data
public class LiveProperties {

    private String pushKey;

    private String pushDomain;

    private String pullDomain;

    private String appName;
}
