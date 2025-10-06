package com.rainbowsea.tidesound.account.service.impl;

import com.rainbowsea.tidesound.account.mapper.RechargeInfoMapper;
import com.rainbowsea.tidesound.account.service.RechargeInfoService;
import com.rainbowsea.tidesound.common.constant.SystemConstant;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.vo.account.RechargeInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

    @Override
    public  Map<String, Object> submitRecharge(RechargeInfoVo rechargeInfoVo) {

        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setUserId(AuthContextHolder.getUserId());
        String orderNo = RandomStringUtils.random(15, false, true);
        rechargeInfo.setOrderNo(orderNo);
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
        int insert = rechargeInfoMapper.insert(rechargeInfo);
        log.info("保存零钱订单状态：{}", insert > 0 ? "success" : "fail");
        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", orderNo);
        return map;
    }
}
