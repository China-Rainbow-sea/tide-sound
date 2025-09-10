package com.rainbowsea.tidesound.common.minio.annotation;

import com.rainbowsea.tidesound.common.minio.config.MinioAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用使用 Minio 工具类，注解的方式
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(MinioAutoConfiguration.class)
public @interface EnableMinioManagement {
}
