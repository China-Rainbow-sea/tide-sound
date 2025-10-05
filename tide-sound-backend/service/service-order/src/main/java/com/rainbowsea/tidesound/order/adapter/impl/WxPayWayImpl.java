package com.rainbowsea.tidesound.order.adapter.impl;


import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.rabbit.service.RabbitService;
import com.rainbowsea.tidesound.order.adapter.PayWay;
import com.rainbowsea.tidesound.order.service.OrderInfoService;
import com.rainbowsea.tidesound.vo.order.OrderInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 微信支付逻辑
 */
@Service
public class WxPayWayImpl implements PayWay {


    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private RabbitService rabbitService;


    @Override
    public boolean isSupport(String payWay) {

        return "1101".equals(payWay);
    }

    @Override
    public void dealPayWay(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        // 微信支付逻辑   后面写
        // 1.保存订单相关的信息
        orderInfoService.saveOrderInfo(orderInfoVo, userId, orderNo);

        // 2.延时(30min)关单  给rabbitmq的延时交换机发送一条消息

        //rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, orderNo,60); // 测试使用
        rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, orderNo,60*30);
        // 线上使用,超时时间 30 分钟

    }



}
