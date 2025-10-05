package com.rainbowsea.tidesound.order.client.impl;


import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.order.client.OrderInfoFeignClient;
import org.springframework.stereotype.Component;

@Component
public class OrderInfoDegradeFeignClient implements OrderInfoFeignClient {

    @Override
    public Result<OrderInfo> getOrderInfoByOrderNo(String orderNo, Long userId) {
        return Result.fail();
    }
}
