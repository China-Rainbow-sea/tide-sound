package com.rainbowsea.tidesound.order.client;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.order.client.impl.OrderInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-order", fallback = OrderInfoDegradeFeignClient.class
,path = "/api/inner/orderinfo")
public interface OrderInfoFeignClient {


    /**
     * 按订单号获取订单信息
     * @param orderNo
     * @param userId
     * @return
     */
    @GetMapping("/getOrderInfoByOrderNo/{orderNo}/{userId}")
    Result<OrderInfo> getOrderInfoByOrderNo(@PathVariable(value = "orderNo") String orderNo, @PathVariable(value = "userId") Long userId);

}