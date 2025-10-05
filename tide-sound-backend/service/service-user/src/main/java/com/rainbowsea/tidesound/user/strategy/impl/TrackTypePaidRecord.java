package com.rainbowsea.tidesound.user.strategy.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.user.UserPaidTrack;
import com.rainbowsea.tidesound.user.service.UserPaidTrackService;
import com.rainbowsea.tidesound.user.strategy.ProcessPaidRecord;
import com.rainbowsea.tidesound.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 声音类型的付款项处理支付流水
 */
@Service(value = "1002")
@Slf4j
public class TrackTypePaidRecord implements ProcessPaidRecord {


    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Override
    public void processDiffItemTypePaidRecord(UserPaidRecordVo userPaidRecordVo) {

        String orderNo = userPaidRecordVo.getOrderNo();
        Long userId = userPaidRecordVo.getUserId();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();

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
}
