package com.rainbowsea.tidesound.account.service.impl;

import com.rainbowsea.tidesound.account.mapper.RechargeInfoMapper;
import com.rainbowsea.tidesound.account.service.RechargeInfoService;
import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

}
