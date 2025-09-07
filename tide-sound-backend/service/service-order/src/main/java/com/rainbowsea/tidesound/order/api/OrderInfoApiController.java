package com.rainbowsea.tidesound.order.api;

import com.rainbowsea.tidesound.order.service.OrderInfoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

}

