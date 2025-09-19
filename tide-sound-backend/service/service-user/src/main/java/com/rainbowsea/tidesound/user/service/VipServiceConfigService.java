package com.rainbowsea.tidesound.user.service;

import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface VipServiceConfigService extends IService<VipServiceConfig> {

    /**
     * 查询应用中所有的vip套餐
     * @return
     */
    List<VipServiceConfig> findAll();
}
