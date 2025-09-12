package com.rainbowsea.tidesound;

import com.rainbowsea.tidesound.common.minio.annotation.EnableMinioManagement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableFeignClients
@EnableMinioManagement
@EnableAspectJAutoProxy(exposeProxy = true)  // 开启 Spring 的AOP 自动代理功能，并确保内部方法调用时也能触发代理逻辑
public class ServiceAlbumApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }

}
