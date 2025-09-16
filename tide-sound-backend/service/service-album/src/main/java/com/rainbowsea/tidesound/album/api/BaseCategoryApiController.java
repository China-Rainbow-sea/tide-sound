package com.rainbowsea.tidesound.album.api;

import com.rainbowsea.tidesound.album.service.BaseCategoryService;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.BaseAttribute;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.vo.category.CategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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


    // Request URL: http://localhost:8500/api/album/category/findAttribute/2
    @GetMapping("/findAttribute/{category1Id}")
    @Operation(summary = "根据一级分类查询专辑的标签信息[属性+属性值]")
    public Result findAttribute(@PathVariable(value = "category1Id") Long category1Id) {

        List<BaseAttribute> baseAttributes = baseCategoryService.findAttribute(category1Id);
        return Result.ok(baseAttributes);
    }



    // Request URL: http://192.168.200.1:8500/api/album/category/findTopBaseCategory3/1
    @GetMapping("/findTopBaseCategory3/{c1Id}")
    @Operation(summary = "根据一级分类id查询置顶的七个三级分类")
    public Result findTopBaseCategory3(@PathVariable(value = "c1Id") Long c1Id) {

        List<BaseCategory3> category3s = baseCategoryService.findTopBaseCategory3(c1Id);
        return Result.ok(category3s);
    }

}

