package com.rainbowsea.tidesound.search.client.impl;


import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.search.client.SearchFeignClient;
import org.springframework.stereotype.Component;

@Component
public class SearchDegradeFeignClient implements SearchFeignClient {


    @Override
    public Result<Boolean> preRankingToCache() {
        return Result.fail();
    }
}
