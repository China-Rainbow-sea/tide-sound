package com.rainbowsea.tidesound.account.service;

public interface MqOpsService {


    /**
     * 初始化初次登录的用户的账户表信息
     * 如果处理账户信息初始化失败,抛出一个异常，被捕获到异常,让其被我们 RabbitMQ 知道,进行一个重试
     * @param content
     */
    void userAccountRegister(String content);

    /**
     * 解锁
     * @param orderNo
     */
    void listenUserAccountUnlock(String orderNo);

    /**
     * 消费
     * @param orderNo
     */
    void listenUserAccountMinus(String orderNo);

    /**
     * 微信支付成功对账户零钱的操作
     * @param userId
     * @param orderNo
     */
    void userAccountOpsRecharge(String userId, String orderNo);
}
