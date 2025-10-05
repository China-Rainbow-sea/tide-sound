package com.rainbowsea.tidesound.order.adapter.impl;

import java.math.BigDecimal;
import java.util.List;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.account.client.UserAccountFeignClient;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.rabbit.service.RabbitService;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.result.ResultCodeEnum;
import com.rainbowsea.tidesound.model.order.LocalMsg;
import com.rainbowsea.tidesound.order.adapter.PayWay;
import com.rainbowsea.tidesound.order.mapper.LocalMsgMapper;
import com.rainbowsea.tidesound.order.service.OrderInfoService;
import com.rainbowsea.tidesound.vo.account.AccountLockResultVo;
import com.rainbowsea.tidesound.vo.account.AccountLockVo;
import com.rainbowsea.tidesound.vo.order.OrderInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 零钱(小程序余额)支付逻辑
 */
@Service
public class RechargePayWayImpl implements PayWay {


    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private LocalMsgMapper localMsgMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;


    String unLockCacheKey = "";

    String minusCacheKey = "";


    /**
     * 定时任务  让发消息给解锁队列一直进行下去。只有等下游说我解锁成功了，定时任务就不发了。
     *
     * @param
     * @return
     */
    @Scheduled(fixedDelay = 1000 * 60)
    public void scanLocalMsgAndRetryMsg() {


        // 1.查询本地消息表，获取到对应消息，没有被标记消息掉的消息，进行重试，定时发送，让其被消费掉
        LambdaQueryWrapper<LocalMsg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocalMsg::getStatus, 0);  // 0 表示没有被消息掉
        // 2.获取到消息没有修改的订单编号
        List<LocalMsg> localMsgs = localMsgMapper.selectList(wrapper);
        for (LocalMsg localMsg : localMsgs) {
            // 3.发送检索消息状态是0的消息 选择发送
            String unLockStr = redisTemplate.opsForValue().get(unLockCacheKey);
            if (!StringUtils.isEmpty(unLockStr)) {
                rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_UNLOCK, localMsg.getMsgContent());
            }
            String minusStr = redisTemplate.opsForValue().get(minusCacheKey);
            if (!StringUtils.isEmpty(minusStr)) {
                rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_MINUS, localMsg.getMsgContent());
            }
        }

    }


    @Override
    public boolean isSupport(String payWay) {

        return "1103".equals(payWay);
    }


    /**
     * 分布式事务的问题：
     * 1、rpc导致的分布式事务：利用异常机制反向修改（Seata）
     * 2、消息队列导致分布式事务。本地消息表+定时任务。
     * <p>
     * 1.使用OpenFeign的时候 由于重试机制，所以会导致接口被重复调用，因此一定要解决由于OpenFeign重试机制的幂等性
     * 2.使用消息队列的时候，由于为了保证消息百分百发送成功，也会引入重试机制（定时任务的重试） 那么也会导致下游消费者会重复消费消息，所以也一定要控制幂等性。
     * <p>
     * 分布式事务有且只有两种情况下会出现
     * 情况一：使用RPC远程调用会导致分布式事务：异常机制+反向操作【反向rpc 反向发消息】 使用Seata的分布式事务框架
     * 情况二：使用消息队列导致的分布式事务：本地消息表+重试机制（定时任务的重试）
     *
     * @param orderInfoVo
     * @param userId
     * @param orderNo
     */
    @Override
    public void dealPayWay(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        // 零钱支付逻辑

        // 1.检查零钱是否充足（并且锁定余额）
        // 如果零钱充足 才保存订单相关信息
        // 如果零钱不充足 不用保存订单相关信息
        // 远程调用账户微服务  只能用rpc 不能用消息队列
        AccountLockVo accountLockVo = prePareUserAccountVo(orderNo, userId, orderInfoVo);
        Result<AccountLockResultVo> lockResultVo = userAccountFeignClient.checkAndLockAmount(accountLockVo); // 锁余额的作用
        if (lockResultVo.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
        }
        // 2.保存订单信息
        try {
            // 2.1 初始化本地消息表
            initLocalMsg(orderNo);
            // 2.2 保存订单信息
            orderInfoService.saveOrderInfo(orderInfoVo, userId, orderNo);
            // 3.解锁以及真正的扣减余额（total_amount以及lock_amount）
            // rpc和发消息都可以
            rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_MINUS, orderNo);

            // 4.处理【零钱】支付成功之后的事情
            orderInfoService.PaySuccess(orderInfoVo, userId, orderNo);
        } catch (GuiguException e) {
            // 反向修改账户微服务下的user_account库对应的表 lock_amount和avliable_amount)
            // PRC  OR  发消息
            rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_UNLOCK, orderNo);
            unLockCacheKey = "unlock:fail:flag" + orderNo;
            redisTemplate.opsForValue().set(unLockCacheKey, "1");
        } catch (Exception e) {
            rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_MINUS, orderNo);
            minusCacheKey = "minus:fail:flag" + orderNo;
            redisTemplate.opsForValue().set(minusCacheKey, "1");
        }


    }

    public void initLocalMsg(String orderNo) {
        LocalMsg localMsg = new LocalMsg();
        localMsg.setMsgContent(orderNo);
        localMsg.setStatus(0);
        localMsgMapper.insert(localMsg);
    }

    private AccountLockVo prePareUserAccountVo(String orderNo, Long userId, OrderInfoVo orderInfoVo) {

        AccountLockVo accountLockVo = new AccountLockVo();
        accountLockVo.setOrderNo(orderNo);
        accountLockVo.setUserId(userId);
        accountLockVo.setAmount(orderInfoVo.getOrderAmount());// 实际买商品要花的钱
        accountLockVo.setContent(orderInfoVo.getOrderDetailVoList().get(0).getItemName());

        return accountLockVo;

    }
}
