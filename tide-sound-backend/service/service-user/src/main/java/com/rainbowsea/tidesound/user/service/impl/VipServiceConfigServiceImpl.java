package com.rainbowsea.tidesound.user.service.impl;

import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.user.mapper.VipServiceConfigMapper;
import com.rainbowsea.tidesound.user.service.VipServiceConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class VipServiceConfigServiceImpl extends ServiceImpl<VipServiceConfigMapper, VipServiceConfig> implements VipServiceConfigService {

	@Autowired
	private VipServiceConfigMapper vipServiceConfigMapper;


}
