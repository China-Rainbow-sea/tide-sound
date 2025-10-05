package com.rainbowsea.tidesound.search.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.model.search.AlbumInfoIndex;
import com.rainbowsea.tidesound.order.client.OrderInfoFeignClient;
import com.rainbowsea.tidesound.search.repository.AlbumInfoIndexRepository;
import com.rainbowsea.tidesound.search.service.ItemService;
import com.rainbowsea.tidesound.search.service.MqOpsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;


/**
 * 处理从RabbitMQ当前上架，下架专辑同步到ES当中的操作（异步）
 */
@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private ItemService itemService;


    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;


    @Override
    public void albumUpper(String albumId) {
        itemService.albumOnSale(Long.parseLong(albumId));
        log.info("上架专辑到es成功!");
    }

    @Override
    public void albumLower(String albumId) {
        itemService.albumOffSale(Long.parseLong(albumId));
        log.info("专辑从es中下架成功!");
    }

    @Override
    public void updateAlbumStatNum(JSONObject jsonObject) {
        String orderNo = (String) jsonObject.get("orderNo");
        Integer userId = (Integer) jsonObject.get("userId");

        // 1.根据订单编号查询订单信息
        Result<OrderInfo> orderInfoByOrderNo = orderInfoFeignClient.getOrderInfoByOrderNo(orderNo, Long.valueOf(userId));
        OrderInfo orderInfo = orderInfoByOrderNo.getData();
        //Assert.notNull(orderInfo, "远程查询订单微服务获取订单信息失败");

        if (orderInfo == null) {
            throw new GuiguException(201, "远程查询订单微服务获取订单信息失败");
        }

        // 2.获取付款项类型
        String itemType = orderInfo.getItemType();
        Long albumId = null;
        if ("1001".equals(itemType)) {
            // 专辑
            albumId = orderInfo.getOrderDetailList().get(0).getItemId();
        } else if ("1002".equals(itemType)) {
            // 声音
            Long trackId = orderInfo.getOrderDetailList().get(0).getItemId();
            Result<TrackInfo> trackInfoResult = albumInfoFeignClient.getTrackInfoByTrackId(trackId);
            TrackInfo trackInfoResultData = trackInfoResult.getData();
            Assert.notNull(trackInfoResultData, "远程查询专辑微服务获取声音信息失败");
            albumId = trackInfoResultData.getAlbumId();
        } else {
            // vip 不用管
            return;
        }


        // 修改es
        Optional<AlbumInfoIndex> albumInfoIndexOptional = albumInfoIndexRepository.findById(albumId);
        albumInfoIndexOptional.ifPresent(albumInfoIndex -> {
            Integer buyStatNum = albumInfoIndex.getBuyStatNum();
            albumInfoIndex.setBuyStatNum(buyStatNum + orderInfo.getOrderDetailList().size());
            albumInfoIndexRepository.save(albumInfoIndex);   // 修改es中的购买量
        });

    }
}
