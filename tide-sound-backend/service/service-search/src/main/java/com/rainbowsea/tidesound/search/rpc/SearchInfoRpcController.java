package com.rainbowsea.tidesound.search.rpc;


import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.search.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */

@RequestMapping("/api/inner/searchinfo")
@RestController
public class SearchInfoRpcController {

    @Autowired
    private ItemService itemService;


    /**
     * 更新 Redis 预缓存的排行榜，更新排行榜
     * @return
     */
    @PutMapping("/preRankingToCache")
    Result<Boolean> preRankingToCache() {

        try {
            itemService.preRankingToCache();
            return Result.ok(true);
        } catch (Exception e) {
            return Result.ok(false);
        }
    }
}
