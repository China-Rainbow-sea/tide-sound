package com.rainbowsea.tidesound.account.mapper;

import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RechargeInfoMapper extends BaseMapper<RechargeInfo> {


    @Update("update   recharge_info  set  recharge_status='0902' WHERE  user_id=#{userId} and recharge_status='0901' and order_no=#{orderNo}")
    int updateRechargeStatus(@Param("userId") String userId, @Param("orderNo") String orderNo);
}
