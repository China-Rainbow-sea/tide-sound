package com.rainbowsea.tidesound.account.service;

import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.vo.account.RechargeInfoVo;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 零钱充值
     *
     * @param rechargeInfoVo
     */
    Map<String, Object> submitRecharge(RechargeInfoVo rechargeInfoVo);
}
