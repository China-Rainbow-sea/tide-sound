package com.rainbowsea.tidesound.album.service;

import com.rainbowsea.tidesound.model.album.BaseAttribute;
import com.rainbowsea.tidesound.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
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


    /**
     * 根据一级分类查询专辑的标签信息[属性+属性值]
     * @param category1Id
     * @return
     */
    List<BaseAttribute> findAttribute(Long category1Id);

    /**
     * 根据专辑id查询专辑的分类id
     *
     * @param albumId
     * @return
     */
    BaseCategoryView getAlbumCategory(Long albumId);

    /**
     * 根据一级分类id查询置顶的七个三级分类
     *
     * @param c1Id
     * @return
     */
    List<BaseCategory3> findTopBaseCategory3(Long c1Id);

    /**
     * 根据一级分类id查询孩子【二级分类和三级分类】
     *
     * @param c1Id
     * @return
     */
    CategoryVo getBaseCategoryListByC1Id(Long c1Id);

    /**
     * 查询全平台的一级分类id集合
     *
     * @return
     */
    List<Long> getAllCategory1Id();
}
