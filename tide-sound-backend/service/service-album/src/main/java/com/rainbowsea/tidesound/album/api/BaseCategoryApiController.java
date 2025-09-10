package com.rainbowsea.tidesound.album.api;

import com.rainbowsea.tidesound.album.service.BaseCategoryService;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.vo.category.CategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album/category")
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;


    // http://localhost:8500/api/album/category/getBaseCategoryList
    @GetMapping("/getBaseCategoryList")
    @Operation(summary = "查询分类信息")
    public Result getBaseCategoryList() {
        List<CategoryVo> categoryList = baseCategoryService.getBaseCategoryList();
        return Result.ok(categoryList);
    }

}

