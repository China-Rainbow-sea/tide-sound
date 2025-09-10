package com.rainbowsea.tidesound.vo.category;

import lombok.Data;

import java.util.List;

/**
 * 封装分类信息的模型
 */

@Data
public class CategoryVo {

    // 分类级别的id,一级分类id，二级分类id,三级分类id,这里我们设计了3级分类
    private Long categoryId;

    // 分类级别的Name,一级分类的 name,二级,三级...
    private String categoryName;


    // 递归思想: 一级分类下的二级分类，二级分类下的三级分类
    private List<CategoryVo> categoryChild;
}
