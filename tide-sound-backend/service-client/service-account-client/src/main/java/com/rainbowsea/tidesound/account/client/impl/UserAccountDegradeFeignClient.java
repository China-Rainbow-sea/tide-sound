package com.rainbowsea.tidesound.account.client.impl;


import com.rainbowsea.tidesound.account.client.UserAccountFeignClient;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.vo.account.AccountLockResultVo;
import com.rainbowsea.tidesound.vo.account.AccountLockVo;
import org.springframework.stereotype.Component;

@Component
public class UserAccountDegradeFeignClient implements UserAccountFeignClient {

    @Override
    public Result<AccountLockResultVo> checkAndLockAmount(AccountLockVo accountLockVo) {

        return Result.fail();
    }
}
