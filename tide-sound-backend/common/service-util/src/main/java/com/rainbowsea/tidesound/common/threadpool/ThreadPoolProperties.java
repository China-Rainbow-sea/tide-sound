package com.rainbowsea.tidesound.common.threadpool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池的设置配置
 */

@Data
@ConfigurationProperties(prefix = "app.threadpool")
public class ThreadPoolProperties {

    private Integer corePoolSize = 2 * Runtime.getRuntime().availableProcessors();// 默认的
    private Integer maxPoolSize = 4 * Runtime.getRuntime().availableProcessors(); // 默认的
    private Integer queueSize = 50;
}
