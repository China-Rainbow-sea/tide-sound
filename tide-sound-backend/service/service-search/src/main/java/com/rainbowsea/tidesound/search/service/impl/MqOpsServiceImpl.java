package com.rainbowsea.tidesound.search.service.impl;

import com.rainbowsea.tidesound.search.service.ItemService;
import com.rainbowsea.tidesound.search.service.MqOpsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 处理从RabbitMQ当前上架，下架专辑同步到ES当中的操作（异步）
 */
@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private ItemService itemService;


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
}
