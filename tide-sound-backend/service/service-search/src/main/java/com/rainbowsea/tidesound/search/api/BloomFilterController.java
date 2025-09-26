package com.rainbowsea.tidesound.search.api;


import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.search.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手动重建布隆过滤器
 */
@RestController
@RequestMapping("api/search/bloom")
public class BloomFilterController {

    @Autowired
    private ItemService itemService;


    @GetMapping("/rebuildBloomFilter")
    @Operation(summary = "手动重建布隆")
    public Result rebuildBloomFilter() {


        Boolean isFlag = itemService.rebuildBloomFilter();

        return Result.ok(isFlag);

    }


}
