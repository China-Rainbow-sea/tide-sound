package com.rainbowsea.tidesound.live.api;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.live.service.LiveTagService;
import com.rainbowsea.tidesound.model.live.LiveTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/live/liveTag")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveTagApiController {

	@Autowired
	private LiveTagService liveTagService;

	@GetMapping("findAllLiveTag")
	public Result<List<LiveTag>> findAllLiveTag(){
		List<LiveTag> list = this.liveTagService.list();
		return Result.ok(list);
	}
}

