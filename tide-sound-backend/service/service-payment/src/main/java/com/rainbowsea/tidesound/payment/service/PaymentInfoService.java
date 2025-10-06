package com.rainbowsea.tidesound.payment.service;

import com.rainbowsea.tidesound.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 保存订单支付信息
     *
     * @param wxPayItemType
     * @param orderNo
     * @return
     */
    PaymentInfo savePaymentInfo(String wxPayItemType, String orderNo);

}
