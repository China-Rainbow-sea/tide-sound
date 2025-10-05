package com.rainbowsea.tidesound.search.service;


import com.alibaba.fastjson.JSONObject;

public interface MqOpsService {
    /**
     * 上架专辑到es
     * @param albumId
     */
    void albumUpper(String albumId);

    /**
     * 从es中下架专辑
     * @param albumId
     */
    void albumLower(String albumId);

    /**
     * 更新专辑的购买量
     *
     * @param jsonObject
     */
    void updateAlbumStatNum(JSONObject jsonObject);
}
