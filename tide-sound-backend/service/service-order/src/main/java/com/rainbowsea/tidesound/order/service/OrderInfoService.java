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

}