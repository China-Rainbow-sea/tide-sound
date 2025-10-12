package com.rainbowsea.tidesound.live.service;

import com.rainbowsea.tidesound.model.live.LiveRoom;
import com.rainbowsea.tidesound.vo.live.LiveRoomVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LiveRoomService extends IService<LiveRoom> {

    LiveRoom saveLiveRoom(LiveRoomVo liveRoomVo);
}
