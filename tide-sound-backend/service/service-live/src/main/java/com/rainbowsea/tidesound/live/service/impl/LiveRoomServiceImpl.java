package com.rainbowsea.tidesound.live.service.impl;

import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.live.config.LiveProperties;
import com.rainbowsea.tidesound.live.mapper.LiveRoomMapper;
import com.rainbowsea.tidesound.live.service.LiveRoomService;
import com.rainbowsea.tidesound.live.utils.LiveAddressUtil;
import com.rainbowsea.tidesound.model.live.LiveRoom;
import com.rainbowsea.tidesound.vo.live.LiveRoomVo;
import com.rainbowsea.tidesound.vo.live.TencentLiveAddressVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveRoomServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveRoomService {

	@Autowired
	private LiveRoomMapper liveRoomMapper;

	@Autowired
	private LiveProperties properties;

	@Autowired
	private LiveAddressUtil liveAddressUtil;

	@Override
	public LiveRoom saveLiveRoom(LiveRoomVo liveRoomVo) {
		Long userId = AuthContextHolder.getUserId();
		// 首先再次验证该主播是否存在未过期的直播间，防止用户通过其他途径绕过前面一步 的查询
		LiveRoom liveRoom = this.getOne(new LambdaQueryWrapper<LiveRoom>()
				.eq(LiveRoom::getUserId, userId).gt(LiveRoom::getExpireTime, new Date()));
		if (liveRoom != null){
			return liveRoom;
		}

		// 如果该用户不存在未过期的直播间则新增直播间
		liveRoom = new LiveRoom();
		BeanUtils.copyProperties(liveRoomVo, liveRoom);

		liveRoom.setUserId(userId);
		liveRoom.setStatus("1");
		liveRoom.setAppName(properties.getAppName());
		// 直播间在腾讯云中的流名称：通过雪花算法生成
		String streamName = IdWorker.getIdStr();
		liveRoom.setStreamName(streamName);
		// 生成直播间的推拉流地址
		TencentLiveAddressVo addressVo = this.liveAddressUtil.getWebRTCLiveAddress(streamName, liveRoom.getExpireTime().getTime() / 1000);
		liveRoom.setPushUrl(addressVo.getPushWebRtcUrl());
		liveRoom.setPlayUrl(addressVo.getPullWebRtcUrl());
		this.save(liveRoom);

		return liveRoom;
	}
}
