package com.rainbowsea.tidesound.account.service;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.vo.account.AccountLockResultVo;
import com.rainbowsea.tidesound.vo.account.AccountLockVo;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 查询用户可用余额
     *
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 检查并且锁定账户余额
     * 不会真正扣减
     *
     * @param accountLockVo
     * @return
     */
    Result<AccountLockResultVo> checkAndLockAmount(AccountLockVo accountLockVo);

    /**
     * 记录用户账户流水
     */
    void log(Long userId, BigDecimal amount, String content, String orderNo, String tradeType);
}