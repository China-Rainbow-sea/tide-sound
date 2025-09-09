package com.rainbowsea.tidesound.user.config.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 微信小程序配置类 appId,appSecret  自动导入的方式
 * 完成配置文件中的属性和该类的属性绑定
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.login")
public class WxAutoProperties {

    private String appId;
    private String appSecret;

}
