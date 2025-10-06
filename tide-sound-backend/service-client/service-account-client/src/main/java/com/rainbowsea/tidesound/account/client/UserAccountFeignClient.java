package com.rainbowsea.tidesound.account.client;

import com.rainbowsea.tidesound.account.client.impl.UserAccountDegradeFeignClient;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.rainbowsea.tidesound.vo.account.AccountLockResultVo;
import com.rainbowsea.tidesound.vo.account.AccountLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-account", fallback = UserAccountDegradeFeignClient.class
,path = "/api/inner/accountinfo")
public interface UserAccountFeignClient {


    /**
     * 查询并锁定金额
     *
     *  1.检查零钱是否充足（并且锁定余额）
     * 如果零钱充足 才保存订单相关信息
     * 如果零钱不充足 不用保存订单相关信息
     * 远程调用账户微服务  只能用rpc 不能用消息队列
     * @param accountLockVo
     * @return
     */
    @PostMapping("/checkAndLockAmount")
    Result<AccountLockResultVo> checkAndLockAmount(@RequestBody AccountLockVo accountLockVo);


    /**
     *按订单号获取充值信息
     * @param orderNo
     * @param userId
     * @return
     */
    @PostMapping("/getRechargeInfoByOrderNo/{orderNo}/{userId}")
    Result<RechargeInfo> getRechargeInfoByOrderNo(@PathVariable(value = "orderNo") String orderNo, @PathVariable(value = "userId") Long userId);

}