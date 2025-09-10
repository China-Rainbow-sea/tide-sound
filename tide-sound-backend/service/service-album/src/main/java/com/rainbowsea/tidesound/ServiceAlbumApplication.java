package com.rainbowsea.tidesound;

import com.rainbowsea.tidesound.common.minio.annotation.EnableMinioManagement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableFeignClients
@EnableMinioManagement
public class ServiceAlbumApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }

}
