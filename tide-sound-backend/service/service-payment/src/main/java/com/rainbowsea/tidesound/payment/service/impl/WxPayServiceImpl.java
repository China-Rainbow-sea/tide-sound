package com.rainbowsea.tidesound.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.rabbit.service.RabbitService;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.payment.PaymentInfo;
import com.rainbowsea.tidesound.payment.config.WxPayV3Config;
import com.rainbowsea.tidesound.payment.mapper.PaymentInfoMapper;
import com.rainbowsea.tidesound.payment.service.PaymentInfoService;
import com.rainbowsea.tidesound.payment.service.WxPayService;
import com.rainbowsea.tidesound.payment.util.PayUtil;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private WxPayV3Config wxPayV3Config;

	@Autowired
	private PaymentInfoService paymentInfoService;

    @Override
    public Map<String, Object> createJsapi(String wxPayItemType, String orderNo) {


        // 0.payment_info表中插入订单支付流水
        PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(wxPayItemType, orderNo);


        // 1.构建config对象
        RSAAutoCertificateConfig config = wxPayV3Config.getConfig();

        // 2.构建service 在利用config对象
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();

        // 3.调用预支付下单接口以及获取微信支付的必要参数
        // // 跟之前下单示例一样，填充预下单参数
        PrepayRequest request = new PrepayRequest();

        // 4.构建请求参数
        Amount amount = new Amount();// 账户对象
        // TODO :生产环境  要根据订单编号去订单数据库中查询这个单的实际金额
        amount.setTotal(1);//  1分钱 要根据订单编号去订单数据库中查询这个单的实际金额（测试数据 1）
        request.setAmount(amount);  // 购买商品的总金额
        request.setAppid(wxPayV3Config.getAppid());  // appid
        request.setMchid(wxPayV3Config.getMerchantId()); // 商户id
        request.setDescription("买了一个飞机");  // 商品内容  在生成阶段：要根据订单编号去订单数据库中查询这个单的名字（标题）
        request.setNotifyUrl(wxPayV3Config.getNotifyUrl());  // 异步回调地址

        request.setOutTradeNo(orderNo);// 给微信平台的订单编号
        Payer payer = new Payer();  // 构建付款者对象
        Result<UserInfoVo> userInfo = userInfoFeignClient.getUserInfo(AuthContextHolder.getUserId());
        UserInfoVo userInfoData = userInfo.getData();
        Assert.notNull(userInfoData, "用户信息不存在");
        payer.setOpenid(userInfoData.getWxOpenId()); // 付款者的openId
        request.setPayer(payer);


        // 5.发起请求 或者微信支付的必要参数
        // response包含了调起支付所需的所有参数，可直接用于前端调起支付
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);

        // 6.解析出来微信支付必要参数
        String packageVal = response.getPackageVal();  // 会话标识
        String timeStamp = response.getTimeStamp();// 时间戳
        String nonceStr = response.getNonceStr(); // 随机字符串
        String signType = response.getSignType(); // 默认RSA,仅支持RSA
        String paySign = response.getPaySign();// 签名算法生成的签名值


        // 7.返回前端
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("timeStamp", timeStamp);
        map.put("nonceStr", nonceStr);
        map.put("package", packageVal);
        map.put("signType", signType);
        map.put("paySign", paySign);


        return map;
    }

    @Override
    public Transaction queryPayStatus(String orderNo, Long userId) {
        try {
            // 1.构建请求对象
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setOutTradeNo(orderNo);
            queryRequest.setMchid(wxPayV3Config.getMerchantId());

            // 2.构建service 在利用config对象
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(wxPayV3Config.getConfig()).build();

            // 3.查询该笔订单支付结果
            Transaction transaction = service.queryOrderByOutTradeNo(queryRequest);

            // 4.返回Transaction对象
            return transaction;
        } catch (ServiceException e) {
            log.error("查询订单支付状态失败：{}", e.getErrorMessage());
            return null;
        }
    }

    @Override
    public void paySuccess(String orderNo, Transaction transaction) {
        // 1.将payment_info表中的支付状态修改为已支付
        // 下商品单支付成功：
        // 2.将order_info中的订单状态修改为已支付
        // 3.像用户支付流水表中插入记录（user_paid_track 或者user_paid_album 或者user_vip_service）
        // 4.修改MySQL中专辑的购买量
        // 5.修改ES中的专辑购买量
        // 充钱成功
        // 2.将recharge_info中的订单状态修改为已支付
        // 3 4 5 不用做了

        // 1.将payment_info表中的支付状态修改为已支付
        int count = paymentInfoMapper.updatePaymentInfoStatus(orderNo);
        if (count > 0) {
            log.info("修改订单的支付状态成功");
        } else {
            log.error("修改订单的支付状态失败");
        }


        // 2.查询该笔订单的支付类型
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));

        String paymentType = paymentInfo.getPaymentType();
//
//        if ("1301".equals(paymentType)) {
//            // 2.将order_info中的订单状态修改为已支付
//            // 3.像用户支付流水表中插入记录（user_paid_track 或者user_paid_album 或者user_vip_service）
//            // 4.修改MySQL中专辑的购买量
//            // 5.修改ES中的专辑购买量
//            rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, MqConst.ROUTING_ORDER_PAY_SUCCESS, orderNo);
//        } else {
//            // 2.将recharge_info中的订单状态修改为已支付
//            // 3 4 5 不用做了
//            rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, MqConst.ROUTING_RECHARGE_PAY_SUCCESS, orderNo);
//        }

        String routingKey = "1301".equals(paymentType) ? MqConst.ROUTING_ORDER_PAY_SUCCESS : MqConst.ROUTING_RECHARGE_PAY_SUCCESS;
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("orderNo", orderNo);

        String openid = transaction.getPayer().getOpenid();
        Result<String> userIdResult = userInfoFeignClient.getUserIdByOpenId(openid);
        String userId = userIdResult.getData();
        msgMap.put("userId", userId);
        String repeatQueryStatusKey = "order:pay:query:lock:" + orderNo;
        // 分布式锁：防止前端30秒主动调用 和 微信回调判断，用户是否支付成功
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(repeatQueryStatusKey, "1", 1, TimeUnit.MINUTES);
        if (aBoolean) {
            rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, routingKey, JSONObject.toJSONString(msgMap));
        }

    }

    @Override
    public Transaction asyncNotify(HttpServletRequest httpServletRequest) {

        // 1.从原始请求的请求头中获取验签和解析要用到的参数
        String signature = httpServletRequest.getHeader("Wechatpay-Signature");   // 微信回调时的签名
        String serial = httpServletRequest.getHeader("Wechatpay-Serial");// 微信平台证书的序列化
        String nonce = httpServletRequest.getHeader("Wechatpay-Nonce");// 签名中的随机数
        String timestamp = httpServletRequest.getHeader("Wechatpay-Timestamp");// 签名中的时间戳

        // 1.验签--验证数据是不是微信平台回调给我的。
        // 2.解析：【解析密文】转成可用的明文

        // 2.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(wxPayV3Config.getConfig());
        String originContent = PayUtil.readData(httpServletRequest);  // 获取微信异步给我的元素报文
        // 3.构建请求参数对象
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(originContent)
                .build();
        try {
            // 4.解析（1验签 2.解密 3.转换成 回调需要用到的Transaction对象
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            return transaction;
        } catch (ValidationException e) {
            log.error("签名验证失败", e.getMessage());
            return null;
        }

    }
}
