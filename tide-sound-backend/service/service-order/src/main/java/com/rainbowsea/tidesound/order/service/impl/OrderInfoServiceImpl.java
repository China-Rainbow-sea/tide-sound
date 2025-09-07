package com.rainbowsea.tidesound.order.service.impl;

import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.order.mapper.OrderInfoMapper;
import com.rainbowsea.tidesound.order.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;


}
