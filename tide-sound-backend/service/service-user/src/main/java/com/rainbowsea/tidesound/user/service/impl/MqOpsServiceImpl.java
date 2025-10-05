package com.rainbowsea.tidesound.user.service.impl;

import java.util.Calendar;
import java.util.Date;


import com.google.common.collect.Maps;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.user.UserInfo;
import com.rainbowsea.tidesound.model.user.UserPaidAlbum;
import com.rainbowsea.tidesound.model.user.UserPaidTrack;
import com.rainbowsea.tidesound.model.user.UserVipService;
import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.user.mapper.UserInfoMapper;
import com.rainbowsea.tidesound.user.mapper.UserPaidAlbumMapper;
import com.rainbowsea.tidesound.user.mapper.UserVipServiceMapper;
import com.rainbowsea.tidesound.user.mapper.VipServiceConfigMapper;
import com.rainbowsea.tidesound.user.service.MqOpsService;
import com.rainbowsea.tidesound.user.service.UserPaidTrackService;
import com.rainbowsea.tidesound.user.strategy.ProcessPaidRecord;
import com.rainbowsea.tidesound.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;


    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;


    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;


    // key,value:"1001",AlbumTypePaidRecord Bean对象
    // key,value:"1002",TrackTypePaidRecord Bean对象
    // key,value:"1003",VipTypePaidRecord Bean对象
    // spring容器默为这个Map赋值：value:ProcessPaidRecord接口的三个实现类 key:这三个实现类指定的Bean的名字
    @Autowired
    private Map<String, ProcessPaidRecord> diffItemTypeMap;


    @Override
    public void listenUserPiedRecord(UserPaidRecordVo userPaidRecordVo) {

        // 1.获取消息的内容
//        Long userId = userPaidRecordVo.getUserId();
//        String orderNo = userPaidRecordVo.getOrderNo();
        String itemType = userPaidRecordVo.getItemType();
//        List<Long> itemIdList = userPaidRecordVo.getItemIdList();

        // 2.根据itemType判断付款项的类型   付款项目类型: 1001-专辑 1002-声音 1003-vip会员

        // 适配器模式不用  策略模式优化
        // 适配器模式和策略模式最大的区别就是：
        // 适配器模式需要一个一个去适配 如果适配到自己 才干活 如果没有适配到自己就不干活
        // 策略模式不需要适配 直接能够找到对应的处理器，然后直接干活。


//        switch (itemType) {
//            case "1001":  // 专辑--user_paid_album表中插入流水
//                dealItemTypeAlbumPaidRecord(orderNo, userId, itemIdList);
//                break;
//            case "1002":  // 专辑--user_paid_track表中插入流水
//                dealItemTypeTrackPaidRecord(orderNo, userId, itemIdList);
//                break;
//            case "1003":  // vip--user_vip_service表中插入流水
//                dealItemTypeVipPaidRecord(orderNo, userId, itemIdList);
//
//        }

        // 根据 Map 中的key找打对应的处理器
        ProcessPaidRecord processPaidRecord = diffItemTypeMap.get(itemType);
        // 根据找到的具体处理器 调用处理方法
        processPaidRecord.processDiffItemTypePaidRecord(userPaidRecordVo);

    }


    /**
     * 处理付款项是专辑类型
     *
     * 被 ProcessPaidRecord 策略模式替代优化掉了
     */

    private void dealItemTypeAlbumPaidRecord(String orderNo, Long userId, List<Long> itemIdList) {


        // 1.幂等性校验（检查该订单是否已经在流水表中 如果在 不用保存 反之才保存）
        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getOrderNo, orderNo);
        wrapper.eq(UserPaidAlbum::getUserId, userId);
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapper);
        if (userPaidAlbum != null) {
            log.error("该专辑类型的流水已经记录过");
            return;
        }
        // 2.保存流水
        userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setOrderNo(orderNo);
        userPaidAlbum.setUserId(userId);
        userPaidAlbum.setAlbumId(itemIdList.get(0));  // 专辑id
        int insert = userPaidAlbumMapper.insert(userPaidAlbum);
        log.info("记录专辑流水：{}", insert > 0 ? "success" : "fail");

    }


    /**
     * 交易项目支付类型为——声音的，订单记录
     *
     * 被 ProcessPaidRecord 策略模式替代优化掉了
     * @param orderNo
     * @param userId
     * @param itemIdList
     */
    private void dealItemTypeTrackPaidRecord(String orderNo, Long userId, List<Long> itemIdList) {

        // 1.幂等性校验（检查该订单是否已经在流水表中 如果在 不用保存 反之才保存）
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getOrderNo, orderNo);
        wrapper.eq(UserPaidTrack::getUserId, userId);
        UserPaidTrack userPaidTrack = userPaidTrackService.getOne(wrapper);
        if (userPaidTrack != null) {
            log.error("该声音类型的流水已经记录过");
            return;
        }

        Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoByTrackId(itemIdList.get(0));
        AlbumInfo albumInfoResultData = albumInfoResult.getData();
        Assert.notNull(albumInfoResultData, "远程调用专辑微服务获取专辑信息失败");

        // 2.保存流水
        List<UserPaidTrack> userPaidTrackList = itemIdList.stream().map(trackId -> {
            UserPaidTrack userPaidTrack1 = new UserPaidTrack();
            userPaidTrack1.setOrderNo(orderNo);
            userPaidTrack1.setUserId(userId);
            userPaidTrack1.setAlbumId(albumInfoResultData.getId());
            userPaidTrack1.setTrackId(trackId);
            return userPaidTrack1;
        }).collect(Collectors.toList());

        boolean b = userPaidTrackService.saveBatch(userPaidTrackList);
        log.info("记录声音流水：{}", b ? "success" : "fail");

    }


    /**
     * 交易项目支付类型为——VIP开通会员的，订单记录
     *
     * 被 ProcessPaidRecord 策略模式替代优化掉了
     * @param orderNo
     * @param userId
     * @param itemIdList
     */
    private void dealItemTypeVipPaidRecord(String orderNo, Long userId, List<Long> itemIdList) {

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
