package com.rainbowsea.tidesound.user.strategy.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.model.user.UserInfo;
import com.rainbowsea.tidesound.model.user.UserVipService;
import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.user.mapper.UserInfoMapper;
import com.rainbowsea.tidesound.user.mapper.UserVipServiceMapper;
import com.rainbowsea.tidesound.user.mapper.VipServiceConfigMapper;
import com.rainbowsea.tidesound.user.strategy.ProcessPaidRecord;
import com.rainbowsea.tidesound.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *  vip类型的付款项处理支付流水
 */
@Service(value = "1003")
@Slf4j
public class VipTypePaidRecord implements ProcessPaidRecord {


    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;


    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Override
    public void processDiffItemTypePaidRecord(UserPaidRecordVo userPaidRecordVo) {

        String orderNo = userPaidRecordVo.getOrderNo();
        Long userId = userPaidRecordVo.getUserId();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();


        // 1.幂等性校验（检查该订单是否已经在流水表中 如果在 不用保存 反之才保存）
        LambdaQueryWrapper<UserVipService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVipService::getOrderNo, orderNo);
        wrapper.eq(UserVipService::getUserId, userId);
        UserVipService userVipService = userVipServiceMapper.selectOne(wrapper);
        if (userVipService != null) {
            log.error("该vip套餐类型的流水已经记录过");
            return;
        }


        // 2.保存
        userVipService = new UserVipService();
        userVipService.setOrderNo(orderNo);
        userVipService.setUserId(userId);
        userVipService.setVipConfigId(itemIdList.get(0).intValue());
        userVipService.setStartTime(new Date());// 套餐开始时间


        // 3.查询套餐信息
        VipServiceConfig vipConfig = vipServiceConfigMapper.selectById(itemIdList.get(0));
        if (vipConfig == null) {
            throw new GuiguException(201, "该套餐信息不存在");
        }

        Integer serviceMonth = vipConfig.getServiceMonth();// 套餐时间

        // 4.查询用户信息
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new GuiguException(201, "该用户信息不存在");
        }

        // 5.获取用户身份
        Integer isVip = userInfo.getIsVip();
        Date vipExpireTime = userInfo.getVipExpireTime();// 获取vip过期时间

        Calendar instance = Calendar.getInstance();  // 得到日历对象
        // 6.不是vip
        if ((isVip + "").equals("0")) {
            instance.setTime(new Date());
        } else {
            // 是vip(过期了)
            if (vipExpireTime.before(new Date())) {
                instance.setTime(new Date());
            } else {
                // 是vip(没过期)
                instance.setTime(vipExpireTime);
            }
        }

        instance.add(Calendar.MONTH, serviceMonth);  //用当前时间+套餐时间
        userVipService.setExpireTime(instance.getTime());  // vip过期时间【当前时间+套餐时间】

        // 7.记录流水
        int insert = userVipServiceMapper.insert(userVipService);
        log.info("记录vip套餐流水：{}", insert > 0 ? "success" : "fail");

        // 8.修改用户身份
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(userVipService.getExpireTime());
        int count = userInfoMapper.updateById(userInfo);
        log.info("修改用户身份以及过期时间：{}", count > 0 ? "success" : "fail");


    }
}
