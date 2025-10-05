package com.rainbowsea.tidesound.user.strategy;


import com.rainbowsea.tidesound.vo.user.UserPaidRecordVo;


/**
 * 策略模式
 * 判断用户购买的是声音，还是专辑，还是VIP
 * 付款项目类型: 1001-专辑 1002-声音 1003-vip会员
 */
public interface ProcessPaidRecord {


    /**
     * 对于不同付款项类型的流水处理方法
     */

    void processDiffItemTypePaidRecord(UserPaidRecordVo userPaidRecordVo);
}
