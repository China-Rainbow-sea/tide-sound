package com.rainbowsea.tidesound.payment.api;

import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.payment.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;


    //  Request URL: http://192.168.200.1:8500/api/payment/wxPay/createJsapi/1301/PBrSdZ4ufok8
    @PostMapping("/createJsapi/{wxPayItemType}/{orderNo}")
    @Operation(summary = "预支付下单")  // 用户可以看到微信的支付二维码。[两个位置都会调到：下商品单或者充值]
    @TingshuLogin
    public Result createJsapi(@PathVariable(value = "wxPayItemType") String wxPayItemType,
                              @PathVariable(value = "orderNo") String orderNo) {

        Map<String, Object> map = wxPayService.createJsapi(wxPayItemType, orderNo);
        return Result.ok(map);
    }



    // Request URL: http://192.168.200.1:8500/api/payment/wxPay/queryPayStatus/H4F2LIiBiHTf
    @GetMapping("/queryPayStatus/{orderNo}")  // 前端主动调用: 前端会在30s内调用
    @Operation(summary = "查询订单的支付状态")
    @TingshuLogin
    public Result queryPayStatus(@PathVariable(value = "orderNo") String orderNo) {

        Transaction transaction = wxPayService.queryPayStatus(orderNo, AuthContextHolder.getUserId());

        if (transaction != null && transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)) {
            // 该笔订单支付成功【微信支付成功】
            // 1.将payment_info表中的支付状态修改为已支付
            // 2.将order_info中的订单状态修改为已支付
            // 3.像用户支付流水表中插入记录（user_paid_track 或者user_paid_album 或者user_vip_service）
            // 4.修改MySQL中专辑的购买量
            // 5.修改ES中的专辑购买量
            wxPayService.paySuccess(orderNo, transaction);
            return Result.ok(true);
        } else {
            return Result.ok(false);
        }

    }


    // api/payment/wxPay/notify

    @PostMapping("/notify")
    @Operation(summary = "异步通知支付结果")  // 微信调用的
    public Map asyncNotify(HttpServletRequest httpServletRequest) {

        System.out.println("异步回调进来了");
        HashMap<String, Object> map = new HashMap<>();

        // Transaction:微信支付平台封状好的数据结果对象
        Transaction transaction = wxPayService.asyncNotify(httpServletRequest);

        // 如果不返回给微信(用户是是否成功还是失败的话：必须是SUCCESS / FAIL 大写的)，就会被一直被24h3m 发送给我们这个回调访问的接口。
        if (transaction != null && transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)) {
            wxPayService.paySuccess(transaction.getOutTradeNo(), transaction);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return map;
        }

        map.put("code", "FAIL");
        map.put("message", "失败");
        return map;
    }

}
