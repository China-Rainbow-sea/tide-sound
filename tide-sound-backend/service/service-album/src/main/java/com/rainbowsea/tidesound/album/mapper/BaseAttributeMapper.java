package com.rainbowsea.tidesound.album.mapper;

import com.rainbowsea.tidesound.model.album.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {


    /**
     * 根据一级分类查询专辑的标签信息
     *    、、@Param("c1Id") Long category1Id 用于在 XML 映射的 SQL 语句当中编写使用
     * @param category1Id
     * @return
     */
    List<BaseAttribute> findAttribute(@Param("c1Id") Long category1Id);
}
