package com.rainbowsea.tidesound.account.api;

import com.rainbowsea.tidesound.account.service.UserAccountService;
import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;


	// Request URL: http://192.168.200.1:8500/api/account/userAccount/getAvailableAmount
	@GetMapping("/getAvailableAmount")
	@Operation(summary = "查询用户可用余额")
	@TingshuLogin
	public Result getAvailableAmount() {

		Long userId = AuthContextHolder.getUserId();
		BigDecimal availableAmount = userAccountService.getAvailableAmount(userId);

		return Result.ok(availableAmount);

	}


}

