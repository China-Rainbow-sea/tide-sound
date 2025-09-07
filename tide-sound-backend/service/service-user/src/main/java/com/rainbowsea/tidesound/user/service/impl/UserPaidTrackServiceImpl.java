package com.rainbowsea.tidesound.user.service.impl;

import com.rainbowsea.tidesound.model.user.UserPaidTrack;
import com.rainbowsea.tidesound.user.mapper.UserPaidAlbumMapper;
import com.rainbowsea.tidesound.user.mapper.UserPaidTrackMapper;
import com.rainbowsea.tidesound.user.service.UserPaidTrackService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserPaidTrackServiceImpl extends ServiceImpl<UserPaidTrackMapper, UserPaidTrack> implements UserPaidTrackService {

	@Autowired
	private UserPaidAlbumMapper userPaidAlbumMapper;

}
