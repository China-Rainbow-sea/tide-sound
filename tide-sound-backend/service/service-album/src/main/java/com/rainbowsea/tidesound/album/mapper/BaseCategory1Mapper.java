package com.rainbowsea.tidesound.album.mapper;

import com.rainbowsea.tidesound.model.album.BaseCategory1;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rainbowsea.tidesound.vo.category.CategoryVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BaseCategory1Mapper extends BaseMapper<BaseCategory1> {


    /**
     * 查询1,2,3级分类：使用 MyBatis 自动封装的方式
     * @return
     */
    List<CategoryVo> getBaseCategoryList();
}
