package com.rainbowsea.tidesound.common.login.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 获取登录信息，认证注解
 */
@Target(ElementType.METHOD)  // 元注解: 表示该注解用于方法中
@Retention(RetentionPolicy.RUNTIME) // 元注解: 表示该注解在什么使用启动有效
public @interface TingshuLogin {
}
