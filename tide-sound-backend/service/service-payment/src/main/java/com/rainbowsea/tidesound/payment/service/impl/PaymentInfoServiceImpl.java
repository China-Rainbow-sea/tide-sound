package com.rainbowsea.tidesound.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.account.client.UserAccountFeignClient;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.model.payment.PaymentInfo;
import com.rainbowsea.tidesound.order.client.OrderInfoFeignClient;
import com.rainbowsea.tidesound.payment.mapper.PaymentInfoMapper;
import com.rainbowsea.tidesound.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {


    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;


    @Override
    public PaymentInfo savePaymentInfo(String wxPayItemType, String orderNo) {
        Long userId = AuthContextHolder.getUserId();

        // 1.根据订单编号查询该笔订单是否支付过

        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderNo, orderNo);
        wrapper.eq(PaymentInfo::getUserId, userId);
//        wrapper.eq(PaymentInfo::getPaymentStatus, "1402");//

        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);

        if (paymentInfo == null) {
            // 记录 ---
            paymentInfo = new PaymentInfo();
            paymentInfo.setUserId(userId);
            paymentInfo.setPaymentType(wxPayItemType);  // 支付类型（下单  充值）
            paymentInfo.setOrderNo(orderNo);
            paymentInfo.setOutTradeNo(orderNo);

            // 下商品单
            if ("1301".equals(wxPayItemType)) {
                Result<OrderInfo> orderInfoByOrderNo = orderInfoFeignClient.getOrderInfoByOrderNo(orderNo, userId);
                OrderInfo orderInfo = orderInfoByOrderNo.getData();
                Assert.notNull(orderInfo, "远程查询订单微服获取订单信息失败");
                String orderInfoPayWay = orderInfo.getPayWay();
                paymentInfo.setPayWay(orderInfoPayWay); // 支付方式（微信 支付宝 零钱）
                paymentInfo.setAmount(orderInfo.getOrderAmount());  // 商品金额
                paymentInfo.setContent(orderInfo.getOrderDetailList().get(0).getItemName());// 商品内容

            } else {
                // 充值钱
                Result<RechargeInfo> rechargeInfoResult = userAccountFeignClient.getRechargeInfoByOrderNo(orderNo, userId);
                RechargeInfo rechargeInfo = rechargeInfoResult.getData();
                Assert.notNull(rechargeInfo, "远程查询账户微服获取充值订单信息失败");
                paymentInfo.setPayWay(rechargeInfo.getPayWay()); // 支付方式（微信 支付宝 零钱）
                paymentInfo.setAmount(rechargeInfo.getRechargeAmount());  // 充值金额
                paymentInfo.setContent("充钱了...");// 商品内容
            }


            paymentInfo.setPaymentStatus("1401"); // 未支付
//            paymentInfo.setCallbackTime(new Date()); // 回调时间
            paymentInfo.setCallbackContent("");// 回调内容
            paymentInfoMapper.insert(paymentInfo);
        }

        // 返回支付对象
        return paymentInfo;

    }
}
