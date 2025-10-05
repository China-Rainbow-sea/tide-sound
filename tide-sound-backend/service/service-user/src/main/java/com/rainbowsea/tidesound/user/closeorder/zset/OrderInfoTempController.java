package com.rainbowsea.tidesound.user.closeorder.zset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付超时，订单关闭：利用 Redis 的 Zset 集合，zadd添加元素以及对应的分数值(订单的过期时间)
 */
@RestController
@RequestMapping("/temp/v1")
public class OrderInfoTempController {

    @Autowired
    private DelayOrderCloseZSetService delayOrderCloseService;


    @PostMapping("/createOrder/{orderId}")
    public String createOrder(@PathVariable(value = "orderId") String orderId) {

        delayOrderCloseService.addDelayOrder(orderId, 60);

        return "success";
    }
}
