package com.rainbowsea.tidesound.search.service;




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
}
