package com.rainbowsea.tidesound.user.api;

import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.user.service.UserListenProcessService;
import com.rainbowsea.tidesound.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;


	// Request URL: http://192.168.200.1:8500/api/user/userListenProcess/getLatelyTrack
	@GetMapping("/getLatelyTrack")
	@TingshuLogin
	@Operation(summary = "用户听最近一次专辑对应的声音")
	public Result getLatelyTrack() {

		Map<Object, Object> map = userListenProcessService.getLatelyTrack();
		return Result.ok(map);

	}

	// Request URL: http://192.168.200.1:8500/api/user/userListenProcess/updateListenProcess
	@PostMapping("/updateListenProcess")
	@TingshuLogin
	@Operation(summary = "更新MongoDB中用户声音的播放进度")
	public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
		userListenProcessService.updateListenProcess(userListenProcessVo);
		return Result.ok();
	}








	// Request URL: http://192.168.200.1:8500/api/user/userListenProcess/getTrackBreakSecond/36362
	@GetMapping("/getTrackBreakSecond/{trackId}")
	@TingshuLogin
	@Operation(summary = "查询已经登录用户的声音【暂停】描述, 获取上次声音播放进度")
	public Result getTrackBreakSecond(@PathVariable(value = "trackId") Long trackId) {

		BigDecimal breakSecond = userListenProcessService.getTrackBreakSecond(trackId);

		return Result.ok(breakSecond);

	}

}

