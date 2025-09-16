package com.rainbowsea.tidesound.search.temp.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// value 可以等于 url,当我们想要使用 OpenFeign 向我们微服务内部(注册到了Nacos)当中服务,value值指明为"注册在Naocs的服务名"即可
// 而如果是想要使用 OpenFeign 对外发送信息,eg:百度，则使用 url 指明发送给外部的那个地址上发送信息,
// value 和 url 同时使用的时候,value 就是作为一个没什么用了，有用的是 url 了
@FeignClient(value = "HelloFeignClient",url = "http://www.baidu.com")
public interface HelloFeignClient {


    /**
     * 定义一个方法(给百度发请求)
     * @GetMapping 命令 feign 发送一个 get 方式发送一个请求给 百度
     * @return
     */
    @GetMapping
    public String sayHello();


    /**
     * 定义一个方法（给百度发请求带参数 哈喽）
     *https://www.baidu.com/s?ie=UTF-8&wd=%E5%93%88%E5%96%BD
     * https://www.baidu.com/s?wd=%E5%93%88%E5%96%BD
     * @GetMapping：命令feign发送一个GET方式的请求
     */
    @GetMapping("/s")
    public String sayHelloWithParam(@RequestParam(value = "wd") String param);
}
