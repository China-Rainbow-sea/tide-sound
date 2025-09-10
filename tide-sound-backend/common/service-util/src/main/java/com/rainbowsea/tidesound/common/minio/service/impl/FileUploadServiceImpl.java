package com.rainbowsea.tidesound.common.minio.service.impl;

import com.rainbowsea.tidesound.common.minio.config.MinioProperties;
import com.rainbowsea.tidesound.common.minio.service.FileUploadService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.util.MD5;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 * 文件上传
 */

//@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioProperties minioProperties;


    // 1. 创建一个 minio 连接； 2.  判断存储桶是否存在 集合操作
    @Autowired
    private MinioClient minioClient;



    @Override
    public String fileUploadService(MultipartFile file) {

        // 去重: 对于文件(就算文件名不同但是内容相同的文件)相同的内容，我们只需要上传一份就可以了
        String objectKey = "";
        String originalFilename = file.getOriginalFilename();
        try {

            // 对文件名进行处理
            byte[] bytes = file.getBytes();  // 将文件转换为 bytes 数组
            String s = new String(bytes);   // 再将 bytes 数组转换为 字符串
            String prefix = MD5.encrypt(s);  // 由于这个转换过来的字符串太长了,进行一个MD5加密处理;MD5特点:相同的内容加密的结果是唯一且一样的

            // 获取的文件名的后缀
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."), originalFilename.length());

            // 拼接上文件: 文件名 + 文件后缀
            objectKey = prefix + suffix;

            // 1.检查该上传的文件是否在minio中
            StatObjectArgs.Builder builder = StatObjectArgs.builder();
            StatObjectArgs statObjectArgs = builder
                    .bucket(minioProperties.getBucketName())
                    .object(objectKey)
                    .build();

            // 文件不存在返回异常，文件存在就没有异常，是通过异常不异常的方式:判断文件是否存在于 minio 当中的
            minioClient.statObject(statObjectArgs);
            // 2.相同内容的文件已经存在了，就将图片地址返回给前端
            // 我们测试了解到的 minio 提供对外访问的图片的地址：http://192.168.76.15:9000/tide-sound/AvB2dxK70lzh9407ff8e12dc7f9d9ccd0df6414dd3b3.jpg
            // 注意需要将存储桶设置为 public 公开的才行
            // IP地址:端口号/存储桶名/文件名.文件后缀
            log.info("该文件内容已经存在了");
            return minioProperties.getEndpointUrl() + "/" + minioProperties.getBucketName() + "/" + objectKey;
        } catch (IOException e) {
            log.error("上传的文件不存在原因：{}", e.getMessage());
            throw new GuiguException(201, "该文件不存在");
        } catch (Exception e) {

            // 3.minio没有 可以上传
            log.info("该文件在桶中不存在，可以上传到minio中");
            try {
                // 4.将文件上传到桶中
                PutObjectArgs.Builder putObjectArgsBuilder = PutObjectArgs.builder();
                // 指明存储桶
                PutObjectArgs putObjectArgs = putObjectArgsBuilder
                        .bucket(minioProperties.getBucketName())
                        .object(objectKey)  // 上传的文件名称(文件对象从前端接受过来的)
                        // 第一个参数是:IO输入流,第二个参数是文件大小,第三个参数是 一次传输的文件大小(部分上传)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .build();
                minioClient.putObject(putObjectArgs);
                log.info("上传文件到minio成功");
                // 2. 将图片地址返回给前端
                // 我们测试了解到的 minio 提供对外访问的图片的地址：http://192.168.76.15:9000/tide-sound/AvB2dxK70lzh9407ff8e12dc7f9d9ccd0df6414dd3b3.jpg
                // 注意需要将存储桶设置为 public 公开的才行
                // IP地址:端口号/存储桶名/文件名.文件后缀
                return minioProperties.getEndpointUrl() + "/" + minioProperties.getBucketName() + "/" + objectKey;
            } catch (Exception subE) {
                log.error("上传文件到minio中失败：{}", subE.getMessage());
                throw new GuiguException(201, "上传文件到minio中失败");
            }
        }
    }

}
