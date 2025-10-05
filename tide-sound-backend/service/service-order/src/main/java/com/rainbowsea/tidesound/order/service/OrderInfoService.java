package com.rainbowsea.tidesound.order.service;

import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.vo.order.OrderInfoVo;
import com.rainbowsea.tidesound.vo.order.TradeVo;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 结算页的展示
     *
     * @param tradeVo
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo);

    /**
     * 提交订单
     *
     * @param orderInfoVo
     * @return
     */
    Map<String, Object> submitOrder(OrderInfoVo orderInfoVo);


    /**
     * 保存订单信息
     *
     * @param orderInfoVo
     * @param userId
     * @param orderNo
     * @return
     */
    OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId, String orderNo);



    /**
     * 支付成功后的事情
     */

    void PaySuccess(OrderInfoVo orderInfoVo, Long userId, String orderNo);



    /**
     * 根据订单编号查询订单
     *
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(String orderNo, Long userId);
}