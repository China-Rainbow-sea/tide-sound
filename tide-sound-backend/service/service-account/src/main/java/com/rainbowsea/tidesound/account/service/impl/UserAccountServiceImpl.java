package com.rainbowsea.tidesound.account.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.account.mapper.RechargeInfoMapper;
import com.rainbowsea.tidesound.account.mapper.UserAccountDetailMapper;
import com.rainbowsea.tidesound.account.mapper.UserAccountMapper;
import com.rainbowsea.tidesound.account.service.UserAccountService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.result.ResultCodeEnum;
import com.rainbowsea.tidesound.model.account.RechargeInfo;
import com.rainbowsea.tidesound.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.model.account.UserAccountDetail;
import com.rainbowsea.tidesound.vo.account.AccountLockResultVo;
import com.rainbowsea.tidesound.vo.account.AccountLockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    @Override
    public BigDecimal getAvailableAmount(Long userId) {

        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);

        UserAccount userAccount = userAccountMapper.selectOne(wrapper);
        return userAccount.getAvailableAmount();
    }

    // 场景：本质 1号用户 锁50元     数据库 1:50
    // SQL1：检查余额
    // select  aviliable_amount  from user_amount where  user_id= #{userId} and aviliable_amount> #{amount}
    // SQL2: 锁定余额
    // update  user_account set  lock_amount=lock_amount+amount, aviliable_amount=aviliable_amount-#{amount} where user_id= #{userId}
    // 一条：CAS思想的SQL层面落地（）
    //   update  user_account set  lock_amount=lock_amount+amount, aviliable_amount=aviliable_amount-#{amount} where user_id= #{userId} and aviliable_amount>= #{amount}

    //  多个人修改：---修改之前先来问一下 当前这个资源是不是符合我预期的 如果是 则修改 如果不是别修改（或者告诉你修改失败）
    // CAS(比较并交换) compare And  Swap
    // 1.查询余额是否充足
    // 伪SQL
    // select  aviliable_amount  from user_amount where  user_id= #{userId} and aviliable_amount> #{amount}
    // 2.1 如果余额不充足 余额不充足 抛出非200的状态码
    // 2.2 如果余额充足 锁定余额
    // 伪SQL
    // update  user_account set  lock_amount=lock_amount+amount, aviliable_amount=aviliable_amount-#{amount} where user_id= #{userId}


    //  未来OpenFeign可能会多次调用
    // 使用OpenFeign的时候一定要注意幂等性保证
    // OpenFeign有重试机制。默认没有使用，但是程序员可以手动打开重试机制  // 底层提供的重试器会重试5次。每次重试间隔会不一样。

    // openFeign在第一次调用的时候 出现了网络抖动（1min）
    // 开启了重试，30s OpenFeign进行第一次重试调用

    // OpenFeign第一次重试时间30s
    // OpenFeign第二次重试时间50s
    // OpenFeign第三次重试时间1min
    // OpenFeign第四次重试时间2min
    // OpenFeign第五次重试时间3min


    // 场景：1号用户 可用余额100块钱
    // 第一次调用1号用户余额充足，扣减50 剩下50   ---
    // 第二次OpenFeign重试继续调用1号用户余额还是充足，又扣减50 剩下0   ---
    // 不满足幂等性

    // 解决：
    // 如果第一个人干期间 第二人不能做（加分布式）
    // 如果第一个人干完了 也不能做（利用缓存的标识）

    // OpenFeign对对超时进行重试 不会对业务执行期间出现的异常进行重试。（单独将）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<AccountLockResultVo> checkAndLockAmount(AccountLockVo accountLockVo) {

        // 1.获取变量
        Long userId = accountLockVo.getUserId();
        BigDecimal amount = accountLockVo.getAmount();
        String content = accountLockVo.getContent();
        String orderNo = accountLockVo.getOrderNo();
        String dataKey = "user:account:amount:" + orderNo;
        String lockKey = "user:account:amount:lock" + orderNo;

        // 2.添加分布式锁
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, "1");

        if (!aBoolean) {
            return Result.build(null, ResultCodeEnum.ACCOUNT_LOCK_REPEAT);// 第一个人还没做完
        }
        // 3.将判断余额是否充足和修改余额作为原子操作利用CAS思想（比较并交换）
        try {
            String result = redisTemplate.opsForValue().get(dataKey);
            if (!StringUtils.isEmpty(result)) {
                return Result.build(JSONObject.parseObject(result, AccountLockResultVo.class), ResultCodeEnum.ACCOUNT_LOCK_REPEAT);//第一个人做过了
            }
            int count = userAccountMapper.checkAndLockAmount(userId, amount);  // SQLException
            // 3.1 如果锁定失败(钱不够)
            if (count == 0) {
                return Result.build(null, ResultCodeEnum.ACCOUNT_LOCK_ERROR);
            }
            // 3.2 锁定成功 构建AccountLockResultVo
            AccountLockResultVo accountLockResultVo = new AccountLockResultVo();
            accountLockResultVo.setUserId(userId);
            accountLockResultVo.setAmount(amount);
            accountLockResultVo.setContent(content);

            // 3.3 记录流水（用户对钱的操作到底干了什么）
            this.log(userId, amount, "锁定：" + content, orderNo, "1202");

            // 3.4 保存到redis中(主要为了解锁和消费的时候取数据方便)
            redisTemplate.opsForValue().set(dataKey, JSONObject.toJSONString(accountLockResultVo));

            // 3.5 返回锁定对象
            return Result.ok(accountLockResultVo);
        } catch (Exception e) {
            // OpenFeign业务执行期间出现了异常 让openFeign进行重试(TODO)
            throw new GuiguException(400, "服务内部处理数据出现了异常");
        } finally {
            redisTemplate.delete(lockKey);   // 3.5 释放锁(一定写在finally)
        }


    }


    @Override
    public void log(Long userId, BigDecimal amount, String content, String orderNo, String tradeType) {


        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(content);
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);


    }

    @Override
    public RechargeInfo getRechargeInfoByOrderNo(String orderNo, Long userId) {

        LambdaQueryWrapper<RechargeInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeInfo::getOrderNo, orderNo);
        wrapper.eq(RechargeInfo::getUserId, userId);
        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(wrapper);
        return rechargeInfo;
    }

    @Override
    public IPage<UserAccountDetail> findUserConsumePage(IPage<UserAccountDetail> page, Long userId) {


        return userAccountDetailMapper.findUserConsumePage(page, userId);
    }

    @Override
    public IPage<UserAccountDetail> findUserRechargePage(IPage<UserAccountDetail> page, Long userId) {

        return userAccountDetailMapper.findUserRechargePage(page, userId);
    }
}
