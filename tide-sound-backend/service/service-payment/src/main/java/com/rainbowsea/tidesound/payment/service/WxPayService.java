package com.rainbowsea.tidesound.payment.service;

import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {


    /**
     * 预支付下单
     *
     * @param wxPayItemType
     * @param orderNo
     * @return
     */
    Map<String, Object> createJsapi(String wxPayItemType, String orderNo);


    /**
     * 查询订单的支付状态
     * @param orderNo
     * @param userId
     * @return
     */
    Transaction queryPayStatus(String orderNo, Long userId);

    /**
     * 微信支付成功后做的事
     * @param orderNo
     * @param transaction
     */
    void paySuccess(String orderNo, Transaction transaction);

    /**
     * 异步通知支付结果
     * @param httpServletRequest
     * @return
     */
    Transaction asyncNotify(HttpServletRequest httpServletRequest);
}
