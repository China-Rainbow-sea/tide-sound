package com.rainbowsea.tidesound.live.api;


import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.live.service.LiveRoomService;
import com.rainbowsea.tidesound.model.live.LiveRoom;
import com.rainbowsea.tidesound.vo.live.LiveRoomVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/live/liveRoom")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveRoomApiController {

	@Autowired
	private LiveRoomService liveRoomService;

	@TingshuLogin
	@GetMapping("getCurrentLive")
	public Result<LiveRoom> getCurrentLive(){
		LiveRoom liveRoom = this.liveRoomService.getOne(new LambdaQueryWrapper<LiveRoom>()
				.eq(LiveRoom::getUserId, AuthContextHolder.getUserId()).gt(LiveRoom::getExpireTime, new Date()));
		return Result.ok(liveRoom);
	}

	@TingshuLogin
	@PostMapping("saveLiveRoom")
	public Result<LiveRoom> saveLiveRoom(@RequestBody LiveRoomVo liveRoomVo){
		LiveRoom liveRoom = this.liveRoomService.saveLiveRoom(liveRoomVo);
		return Result.ok(liveRoom);
	}

	@GetMapping("getById/{liveRoomId}")
	public Result<LiveRoom> getById(@PathVariable Long liveRoomId){
		LiveRoom liveRoom = this.liveRoomService.getById(liveRoomId);
		return Result.ok(liveRoom);
	}

	@GetMapping("findLiveList")
	public Result<List<LiveRoom>> findLiveList(){
		List<LiveRoom> liveRooms = this.liveRoomService.list(new LambdaQueryWrapper<LiveRoom>().gt(LiveRoom::getExpireTime, new Date()));
		return Result.ok(liveRooms);
	}
}

