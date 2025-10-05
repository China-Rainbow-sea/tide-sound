package com.rainbowsea.tidesound.order.service;

/**
 * 本地数据表：
 *
 * 消息队列导致分布式事务，本地消息表 +定时任务；消息没有被消费，
 * 就让生产者一直发，直到被消费者消费掉消息为止。利用本地消息表进行一个标记。
 */
public interface MqOpsService {
    /**
     * 修改本地消息表的状态
     *
     * @param content
     */
    void updateLocalMsgStatus(String content);

    /**
     * 关闭订单
     * @param orderNo
     */
    void cancelOrder(String orderNo);
}
