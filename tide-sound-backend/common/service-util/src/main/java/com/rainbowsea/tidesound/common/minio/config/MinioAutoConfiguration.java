package com.rainbowsea.tidesound.common.minio.config;


import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.minio.service.FileUploadService;
import com.rainbowsea.tidesound.common.minio.service.impl.FileUploadServiceImpl;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 创建 minio 连接，同时判断存储桶是否存在
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(prefix = "minio",value = "enable",havingValue = "true")
public class MinioAutoConfiguration {

    // minio 配置类
    @Autowired
    private MinioProperties minioProperties;

    // 变量的方式引入：Slf4j 当中 logger 打印日志
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public MinioClient minioClient() {


        try {
            // 1.创建 minio客户端
            MinioClient minioClient =
                    MinioClient.builder()
                            // minio 存储位置
                            .endpoint(minioProperties.getEndpointUrl())
                            // minio 存储服务的账户和密码
                            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                            .build();

            // 2.判断桶是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!found) {
                // 3.创建桶
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            } else {
                logger.info("桶已经存在");
            }

            return minioClient;
        } catch (Exception e) {
            logger.error("minioClient对象创建失败：{}", e.getMessage());
            throw new GuiguException(201, "minioClient对象创建失败");
        }
    }


    /**
     * 加入到 IOC 容器当中，同时取消到 FileUploadServiceImpl 类上的 @Service 注解，
     * 通过引入该  MinioAutoConfiguration 就可以间接的引入了  FileUploadServiceImpl 了。
     * 前提是该 FileUploadServiceImpl 也是被可以加入到 IOC 容器当中的。
     * @return
     */
    @Bean
    public FileUploadService fileUploadService(){
        return new FileUploadServiceImpl();
    }
}
