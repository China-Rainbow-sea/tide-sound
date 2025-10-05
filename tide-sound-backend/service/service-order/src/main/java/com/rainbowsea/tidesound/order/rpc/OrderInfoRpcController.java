package com.rainbowsea.tidesound.order.rpc;


import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.order.service.OrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@RestController
@RequestMapping("/api/inner/orderinfo")
public class OrderInfoRpcController {

    @Autowired
    private OrderInfoService orderInfoService;


    /**
     * 按订单号获取订单信息
     * @param orderNo
     * @param userId
     * @return
     */
    @GetMapping("/getOrderInfoByOrderNo/{orderNo}/{userId}")
    Result<OrderInfo> getOrderInfoByOrderNo(@PathVariable(value = "orderNo") String orderNo, @PathVariable(value =
            "userId") Long userId) {
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo, userId);
        return Result.ok(orderInfo);
    }




}
