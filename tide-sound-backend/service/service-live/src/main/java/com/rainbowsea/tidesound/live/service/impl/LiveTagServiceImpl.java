package com.rainbowsea.tidesound.live.service.impl;

import com.rainbowsea.tidesound.live.mapper.LiveTagMapper;
import com.rainbowsea.tidesound.live.service.LiveTagService;
import com.rainbowsea.tidesound.model.live.LiveTag;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveTagServiceImpl extends ServiceImpl<LiveTagMapper, LiveTag> implements LiveTagService {

	@Autowired
	private LiveTagMapper liveTagMapper;

}
