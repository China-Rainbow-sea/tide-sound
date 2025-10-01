package com.rainbowsea.tidesound.order.api;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.order.service.OrderInfoService;
import com.rainbowsea.tidesound.vo.order.OrderInfoVo;
import com.rainbowsea.tidesound.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;


	// Request URL: http://192.168.200.1:8500/api/order/orderInfo/trade
	@PostMapping("/trade")
	@TingshuLogin
	@Operation(summary = "结算页的展示")
	public Result trade(@RequestBody TradeVo tradeVo) {

		OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo);

		List<Long> exitItemIdList = orderInfoVo.getExitItemIdList();
		if (!CollectionUtils.isEmpty(exitItemIdList)) {
			return Result.fail(orderInfoVo);
		}

		return Result.ok(orderInfoVo);
	}




	// Request URL: http://192.168.200.1:8500/api/order/orderInfo/submitOrder
	@PostMapping("/submitOrder")
	@TingshuLogin
	@Operation(summary = "提交订单")
	public Result submitOrder(@RequestBody @Validated OrderInfoVo orderInfoVo) {

		Map<String, Object> orderNoMap = orderInfoService.submitOrder(orderInfoVo);

		return Result.ok(orderNoMap);
	}

}

