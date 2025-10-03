package com.rainbowsea.tidesound.order.adapter;


import com.rainbowsea.tidesound.vo.order.OrderInfoVo;

/**
 * 支付方式的接口
 */
public interface PayWay {

    /**
     * 定义适配某一种具体支付方式的适配方式
     */
    public boolean isSupport(String payWay);


    /**
     * 具体支付方式的处理支付逻辑
     */
    public void dealPayWay(OrderInfoVo orderInfoVo, Long userId, String orderNo);
}