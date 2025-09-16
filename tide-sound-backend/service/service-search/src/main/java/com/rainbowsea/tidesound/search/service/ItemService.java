package com.rainbowsea.tidesound.search.service;

public interface ItemService {



    /**
     * 专辑的上架
     *
     * @param albumId
     */
    void albumOnSale(Long albumId);

    /**
     * 专辑的下架
     *
     * @param albumId
     */
    void albumOffSale(Long albumId);


    /**
     * 专辑批量的下架
     */
    void batchAlbumOffSale();
}
