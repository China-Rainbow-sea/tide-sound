package com.rainbowsea.tidesound.payment.service.impl;

import com.rainbowsea.tidesound.model.payment.PaymentInfo;
import com.rainbowsea.tidesound.payment.mapper.PaymentInfoMapper;
import com.rainbowsea.tidesound.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
