package com.rainbowsea.tidesound.search.client;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-search", fallback = SearchDegradeFeignClient.class,
        path = "/api/inner/searchinfo")
public interface SearchFeignClient {


    /**
     * 更新Redis 当中预存的排行榜数据
     * @return
     */
    @PutMapping("/preRankingToCache")
    Result<Boolean> preRankingToCache();

}