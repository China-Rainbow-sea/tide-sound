package com.rainbowsea.tidesound.user.service;


import com.rainbowsea.tidesound.vo.user.UserPaidRecordVo;

public interface MqOpsService {


    /**
     * RabbitMQ 监听 用户支付记录
     * @param userPaidRecordVo
     */
    void listenUserPiedRecord(UserPaidRecordVo userPaidRecordVo);
}
