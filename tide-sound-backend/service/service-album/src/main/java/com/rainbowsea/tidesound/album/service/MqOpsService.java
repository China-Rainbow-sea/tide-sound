package com.rainbowsea.tidesound.album.service;


import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.vo.album.TrackStatMqVo;

/**
 *
 */
public interface MqOpsService {

    /**
     * 更新声音不同维度的值
     * @param trackStatMqVo
     */
    void trackStatTypeUpdate(TrackStatMqVo trackStatMqVo);

    /**
     * 更新专辑的购买量
     *
     * @param jsonObject
     */
    void albumStatTypeUpdate(JSONObject jsonObject);
}
