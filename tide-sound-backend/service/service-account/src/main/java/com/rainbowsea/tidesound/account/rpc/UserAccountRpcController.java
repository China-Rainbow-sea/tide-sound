package com.rainbowsea.tidesound.account.rpc;


import com.rainbowsea.tidesound.account.service.UserAccountService;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.rainbowsea.tidesound.vo.account.AccountLockResultVo;
import com.rainbowsea.tidesound.vo.account.AccountLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */

@RestController
@RequestMapping("/api/inner/accountinfo")
public class UserAccountRpcController {

    @Autowired
    private UserAccountService userAccountService;


    @PostMapping("/checkAndLockAmount")
    Result<AccountLockResultVo> checkAndLockAmount(@RequestBody AccountLockVo accountLockVo) {

        return userAccountService.checkAndLockAmount(accountLockVo);
    }


    /**
     * 根据充值订单编号查询订单信息
     * @param orderNo
     * @param userId
     * @return
     */
    @PostMapping("/getRechargeInfoByOrderNo/{orderNo}/{userId}")
    Result<RechargeInfo> getRechargeInfoByOrderNo(@PathVariable(value = "orderNo") String orderNo, @PathVariable(value = "userId") Long userId) {

        RechargeInfo rechargeInfo = userAccountService.getRechargeInfoByOrderNo(orderNo, userId);
        return Result.ok(rechargeInfo);

    }
}
