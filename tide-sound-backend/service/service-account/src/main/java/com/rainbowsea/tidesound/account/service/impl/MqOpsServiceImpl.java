package com.rainbowsea.tidesound.account.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.account.mapper.UserAccountMapper;
import com.rainbowsea.tidesound.account.service.MqOpsService;
import com.rainbowsea.tidesound.account.service.UserAccountService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.model.account.UserAccount;
import com.rainbowsea.tidesound.vo.account.AccountLockResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;


@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {


    @Autowired
    private UserAccountMapper userAccountMapper;


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserAccountService userAccountService;


    @Override
    public void userAccountRegister(String content) {
        try {
            UserAccount userAccount = new UserAccount();
            userAccount.setUserId(Long.parseLong(content));
            int insert = userAccountMapper.insert(userAccount);


            log.info("初始化用户账户：{}", insert > 0 ? "success" : "fail");

        } catch (Exception e) {
            throw new GuiguException(201, "服务内部处理数据失败");
        }

    }



    @Override
    public void listenUserAccountUnlock(String orderNo) {


        //  修改 lock_amount(-) 以及avaliaabl_amount(+)

        // 1、从Redis获取锁定的对象
        String dataKey = "user:account:amount:" + orderNo;
        String lockKey = "user:account:amount:lock:retry" + orderNo;
        String lockAmountResultStr = redisTemplate.opsForValue().get(dataKey);

        if (StringUtils.isEmpty(lockAmountResultStr)) {   // redis的标识
            return;
        }
        AccountLockResultVo accountLockResultVo = JSONObject.parseObject(lockAmountResultStr, AccountLockResultVo.class);
        Long userId = accountLockResultVo.getUserId();
        BigDecimal amount = accountLockResultVo.getAmount();
        String content = accountLockResultVo.getContent();
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, "1");
        if (aBoolean) {
            // 2.解锁
            try {
                int count = userAccountMapper.unLock(userId, amount);
                // 3.解锁记录存放到流水表中
                userAccountService.log(userId, amount, "解锁：" + content, orderNo, "1203-解锁");

                redisTemplate.delete(dataKey);// 正常干完活才能删除缓存标识key

            } catch (Exception e) {
                throw new GuiguException(201, "解锁失败");
            } finally {
                // 4.删除缓存中的锁定对象
                redisTemplate.delete(lockKey);// 不管正常还是异常都要把锁key给删除掉
            }


        }


    }

    @Override
    public void listenUserAccountMinus(String orderNo) {


        //  修改 lock_amount(-) 以及total_amount(-)

        // 1、从Redis获取锁定的对象
        String dataKey = "user:account:amount:" + orderNo;
        String lockKey = "user:account:minus:lock:" + orderNo;
        String lockAmountResultStr = redisTemplate.opsForValue().get(dataKey);

        if (StringUtils.isEmpty(lockAmountResultStr)) {
            return;
        }
        AccountLockResultVo accountLockResultVo = JSONObject.parseObject(lockAmountResultStr, AccountLockResultVo.class);
        Long userId = accountLockResultVo.getUserId();
        BigDecimal amount = accountLockResultVo.getAmount();
        String content = accountLockResultVo.getContent();
        // 2.解锁
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, "1");
        if (aBoolean) {
            try {
                int count = userAccountMapper.minus(userId, amount);
                // 3.解锁记录存放到流水表中
                userAccountService.log(userId, amount, "消费：" + content, orderNo, "1204-消费");

                // 4.删除缓存中的锁定对象
                redisTemplate.delete(dataKey);
            } catch (Exception e) {
                throw new GuiguException(201, "消费失败");
            } finally {
                redisTemplate.delete(lockKey);
            }


        }


    }
}
