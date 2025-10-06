package com.rainbowsea.tidesound.account.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.rainbowsea.tidesound.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.model.account.UserAccountDetail;
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

    /**
     * 根据充值订单编号查询订单信息
     *
     * @param orderNo
     * @param userId
     * @return
     */
    RechargeInfo getRechargeInfoByOrderNo(String orderNo, Long userId);


    /**
     * 查询用户的消费记录
     *
     * @param page
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserConsumePage(IPage<UserAccountDetail> page, Long userId);


    /**
     * 查询用户的充值记录
     *
     * @param page
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserRechargePage(IPage<UserAccountDetail> page, Long userId);

}