package com.rainbowsea.tidesound.album.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.album.mapper.AlbumStatMapper;
import com.rainbowsea.tidesound.album.mapper.TrackInfoMapper;
import com.rainbowsea.tidesound.album.mapper.TrackStatMapper;
import com.rainbowsea.tidesound.album.service.MqOpsService;
import com.rainbowsea.tidesound.common.constant.SystemConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.order.client.OrderInfoFeignClient;
import com.rainbowsea.tidesound.vo.album.TrackStatMqVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 */
@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private TrackStatMapper trackStatMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;


    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;



    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void trackStatTypeUpdate(TrackStatMqVo trackStatMqVo) {

        // 1.获取参数
        Long trackId = trackStatMqVo.getTrackId();
        Long albumId = trackStatMqVo.getAlbumId();
        String statType = trackStatMqVo.getStatType();
        Integer count = trackStatMqVo.getCount();

        try {

            if (albumId != null) {
                // 2.更新专辑的播放量（没有收藏量）
                int affectAlbumUpdateRows = albumStatMapper.updateAlbumNumByType(albumId, SystemConstant.ALBUM_STAT_PLAY, count);
                log.info("更新专辑的播放量：{}", affectAlbumUpdateRows > 0 ? "成功" : "失败");
            }
            // 3.更新声音的播放量/收藏量
            int affectTrackUpdateRows = trackStatMapper.updateTrackNumByType(trackId, statType, count);
            log.info("更新声音的播放量：{}", affectTrackUpdateRows > 0 ? "成功" : "失败");

        } catch (Exception e) {
            throw new GuiguException(201, "更新声音的播放量失败");
        }
    }

    @Override
    public void albumStatTypeUpdate(JSONObject jsonObject) {
        String orderNo = (String) jsonObject.get("orderNo");
        Integer userId = (Integer) jsonObject.get("userId");

        // 1.根据订单编号查询订单信息
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfoByOrderNo(orderNo, Long.valueOf(userId));
        OrderInfo orderInfoResultData = orderInfoResult.getData();


        //Assert.notNull(orderInfoResultData, "远程查询订单微服务获取订单信息失败");

        if (orderInfoResultData == null ) {
            throw new GuiguException(201, "远程查询订单微服务获取订单信息失败");
        }


        // 2.获取付款项类型
        String itemType = orderInfoResultData.getItemType();
        Long albumId = null;


        if ("1001".equals(itemType)) {
            // 处理专辑
            albumId = orderInfoResultData.getOrderDetailList().get(0).getItemId();
        } else if ("1002".equals(itemType)) {
            // 处理 声音---修改这个声音对应专辑的购买量
            Long trackId = orderInfoResultData.getOrderDetailList().get(0).getItemId();
            TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
            albumId = trackInfo.getAlbumId();
        } else {
            return;  // vip不用管
        }

        albumStatMapper.updateAlbumNumByType(albumId, "0403", orderInfoResultData.getOrderDetailList().size());


    }
}
