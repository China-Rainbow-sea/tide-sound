package com.rainbowsea.tidesound.account.api;

import com.rainbowsea.tidesound.account.service.RechargeInfoService;
import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account/rechargeInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;


	// Request URL: http://192.168.200.1:8500/api/account/rechargeInfo/submitRecharge
	@PostMapping("/submitRecharge")
	@Operation(summary = "零钱充值")
	@TingshuLogin
	public Result submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {

		Map<String, Object> map = rechargeInfoService.submitRecharge(rechargeInfoVo);

		return Result.ok(map);
	}

}

