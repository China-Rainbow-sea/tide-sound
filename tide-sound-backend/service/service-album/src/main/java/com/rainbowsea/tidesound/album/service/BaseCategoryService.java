package com.rainbowsea.tidesound.album.service;

import com.rainbowsea.tidesound.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.vo.category.CategoryVo;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 查询分类级别信息
     * 采用：MyBatis SQL映射的方式
     *
     * @return
     */
    List<CategoryVo> getBaseCategoryList();


    /**
     * 优化，可以使用递归的方式。
     * 这里使用的是循环遍历的方式赋值。
     * @return
     */
    public List<CategoryVo> getBaseCategoryList2();
}
