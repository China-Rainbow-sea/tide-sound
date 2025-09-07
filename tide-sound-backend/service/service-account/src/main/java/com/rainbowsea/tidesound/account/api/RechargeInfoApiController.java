package com.rainbowsea.tidesound.account.api;

import com.rainbowsea.tidesound.account.service.RechargeInfoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account/rechargeInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;

}

