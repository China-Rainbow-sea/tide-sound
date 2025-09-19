package com.rainbowsea.tidesound.user.api;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user/vipServiceConfig")
@SuppressWarnings({"unchecked", "rawtypes"})
public class VipServiceConfigApiController {

	@Autowired
	private VipServiceConfigService vipServiceConfigService;


	// Request URL: http://192.168.200.1:8500/api/user/vipServiceConfig/findAll
	@GetMapping("/findAll")
	@Operation(summary = "查询应用中所有的vip套餐")
	public Result findAll() {
		List<VipServiceConfig> vipServiceConfigList = vipServiceConfigService.findAll();
		return Result.ok(vipServiceConfigList);

	}

}

