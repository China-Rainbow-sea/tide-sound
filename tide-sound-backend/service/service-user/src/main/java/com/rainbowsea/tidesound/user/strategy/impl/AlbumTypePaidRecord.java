package com.rainbowsea.tidesound.user.strategy.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rainbowsea.tidesound.model.user.UserPaidAlbum;
import com.rainbowsea.tidesound.user.mapper.UserPaidAlbumMapper;
import com.rainbowsea.tidesound.user.strategy.ProcessPaidRecord;
import com.rainbowsea.tidesound.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 专辑类型的付款项处理支付流水
 */
@Service(value = "1001")
@Slf4j
public class AlbumTypePaidRecord implements ProcessPaidRecord {


    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Override
    public void processDiffItemTypePaidRecord(UserPaidRecordVo userPaidRecordVo) {


        // 1.幂等性校验（检查该订单是否已经在流水表中 如果在 不用保存 反之才保存）
        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo());
        wrapper.eq(UserPaidAlbum::getUserId, userPaidRecordVo.getUserId());
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapper);
        if (userPaidAlbum != null) {
            log.error("该专辑类型的流水已经记录过");
            return;
        }
        // 2.保存流水
        userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));  // 专辑id
        int insert = userPaidAlbumMapper.insert(userPaidAlbum);
        log.info("记录专辑流水：{}", insert > 0 ? "success" : "fail");


    }
}
