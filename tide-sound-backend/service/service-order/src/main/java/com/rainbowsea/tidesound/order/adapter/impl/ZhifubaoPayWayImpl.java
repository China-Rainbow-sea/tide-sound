package com.rainbowsea.tidesound.order.adapter.impl;


import com.rainbowsea.tidesound.order.adapter.PayWay;
import com.rainbowsea.tidesound.vo.order.OrderInfoVo;
import org.springframework.stereotype.Service;

/**
 * 支付宝支付逻辑
 */
@Service
public class ZhifubaoPayWayImpl implements PayWay {
    @Override
    public boolean isSupport(String payWay) {

        return "1102".equals(payWay);
    }

    @Override
    public void dealPayWay(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        // 支付宝支付逻辑  TODO
    }

}
