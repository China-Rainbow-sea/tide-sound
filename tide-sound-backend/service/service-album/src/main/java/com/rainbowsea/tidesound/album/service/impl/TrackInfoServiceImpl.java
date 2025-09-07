package com.rainbowsea.tidesound.album.service.impl;

import com.rainbowsea.tidesound.album.mapper.TrackInfoMapper;
import com.rainbowsea.tidesound.album.service.TrackInfoService;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

}
