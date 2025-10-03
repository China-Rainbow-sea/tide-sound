package com.rainbowsea.tidesound.order.adapter.impl;


import com.rainbowsea.tidesound.order.adapter.PayWay;
import com.rainbowsea.tidesound.vo.order.OrderInfoVo;
import org.springframework.stereotype.Service;

/**
 * 微信支付逻辑
 */
@Service
public class WxPayWayImpl implements PayWay {
    @Override
    public boolean isSupport(String payWay) {

        return "1101".equals(payWay);
    }

    @Override
    public void dealPayWay(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        // 微信支付逻辑   后面写
    }


}
