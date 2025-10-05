package com.rainbowsea.tidesound.order.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.model.order.LocalMsg;
import com.rainbowsea.tidesound.order.mapper.LocalMsgMapper;
import com.rainbowsea.tidesound.order.mapper.OrderInfoMapper;
import com.rainbowsea.tidesound.order.service.MqOpsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 本地数据表
 * 消息队列导致分布式事务，本地消息表 +定时任务；消息没有被消费，
 * 就让生产者一直发，直到被消费者消费掉消息为止。利用本地消息表进行一个标记。
 */

@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private LocalMsgMapper localMsgMapper;


    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    public void updateLocalMsgStatus(String content) {

        LambdaQueryWrapper<LocalMsg> wrapper = new LambdaQueryWrapper<LocalMsg>();
        wrapper.eq(LocalMsg::getMsgContent, content);
        LocalMsg localMsg = localMsgMapper.selectOne(wrapper);
        if (localMsg != null) {
            localMsg.setStatus(1);
            localMsgMapper.updateById(localMsg);
        }
    }

    @Override
    public void cancelOrder(String orderNo) {

        int count = orderInfoMapper.cancelOrder(orderNo);

        log.info("关闭订单：{}",count>0?"success":"fail");
    }
}
