package com.rainbowsea.tidesound.album.service.impl;

import com.rainbowsea.tidesound.album.mapper.AlbumInfoMapper;
import com.rainbowsea.tidesound.album.service.AlbumInfoService;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;
}
