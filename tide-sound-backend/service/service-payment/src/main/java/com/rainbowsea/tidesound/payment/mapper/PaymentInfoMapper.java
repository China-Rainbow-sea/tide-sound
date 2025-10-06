package com.rainbowsea.tidesound.payment.mapper;

import com.rainbowsea.tidesound.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentInfoMapper extends BaseMapper<PaymentInfo> {


    /**
     * 将payment_info表中的支付状态修改为已支付
     * @param orderNo
     * @return
     */
    @Update("update   payment_info  set  payment_status='1402' WHERE  \n" +
            "payment_info.payment_status='1401' and payment_info.order_no=#{orderNo}")
    int updatePaymentInfoStatus(@Param("orderNo") String orderNo);


}
