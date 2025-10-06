package com.rainbowsea.tidesound.account.mapper;

import com.rainbowsea.tidesound.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {


    /**
     * 检查并锁定金额
     * @param userId
     * @param amount
     * @return
     */
    int checkAndLockAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);


    /**
     *解锁
     * @param userId
     * @param amount
     * @return
     */
    int unLock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);


    /**
     * 消费锁
     * @param userId
     * @param amount
     * @return
     */
    int minus(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int updateUserAccount(@Param("userId") String userId, @Param("amount") BigDecimal rechargeAmount);

}
