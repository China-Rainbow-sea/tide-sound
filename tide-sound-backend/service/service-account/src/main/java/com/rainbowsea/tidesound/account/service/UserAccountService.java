package com.rainbowsea.tidesound.account.service;

import com.rainbowsea.tidesound.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {



    /**
     * 查询用户可用余额
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);
}
