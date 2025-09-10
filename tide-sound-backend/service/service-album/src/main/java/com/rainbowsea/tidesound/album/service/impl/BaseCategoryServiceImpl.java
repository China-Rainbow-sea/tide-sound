package com.rainbowsea.tidesound.album.service.impl;

import com.google.common.collect.Lists;
import com.rainbowsea.tidesound.album.mapper.BaseCategory1Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategory2Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategory3Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategoryViewMapper;
import com.rainbowsea.tidesound.album.service.BaseCategoryService;
import com.rainbowsea.tidesound.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.rainbowsea.tidesound.vo.category.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    // 操作 tingshu_album 数据库下的 base_category_view 视图
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;


    /**
     * 优化，可以使用递归的方式。
     * 这里使用的是循环遍历的方式赋值。
     *
     * @return
     */
    @Override
    public List<CategoryVo> getBaseCategoryList() {
        return baseCategory1Mapper.getBaseCategoryList();
    }


    /**
     * 优化，可以使用递归的方式。
     * 这里使用的是循环遍历的方式赋值。
     *
     * @return
     */
    @Override
    public List<CategoryVo> getBaseCategoryList2() {

        List<CategoryVo> result = new ArrayList<>();
        // 1. 查询所有的分类信息(null 表示查询全部信息)
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);


        // 2. 封装数据Collectors.groupingBy(BaseCategoryView::getCategory1Id)(根据一级分类ID分组)
        Map<Long, List<BaseCategoryView>> category1IdAndValue = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        for (Map.Entry<Long, List<BaseCategoryView>> longListEntry : category1IdAndValue.entrySet()) {
            // 1. 定义一个一级分类
            CategoryVo category1Vo = new CategoryVo();

            category1Vo.setCategoryId(longListEntry.getKey());  // 给 1级分类 id 赋值
            category1Vo.setCategoryName(longListEntry.getValue().get(0).getCategory1Name());  // 给 1级分类名字赋值,get(0)
            // 获取的第一个参数就是1级分类的Name

            // 给二级分类赋值Collectors.groupingBy(BaseCategoryView::getCategory2Id)(根据2级分类ID分组)
            Map<Long, List<BaseCategoryView>> category2IdAndValue = longListEntry.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

            // 2级分类处理，封装
            List<CategoryVo> category1Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> listEntry : category2IdAndValue.entrySet()) {
                // 2. 定义一个二级分类
                CategoryVo category2Vo = new CategoryVo();  // 给 2 级分类 id 赋值
                category2Vo.setCategoryId(listEntry.getKey());
                category2Vo.setCategoryName(listEntry.getValue().get(0).getCategory2Name());


                List<CategoryVo> category2Child = new ArrayList<>();
                // 3级分类处理,封装
                for (BaseCategoryView baseCategoryView : listEntry.getValue()) {
                    // 2. 定义一个3级分类
                    CategoryVo category3Vo = new CategoryVo();
                    category3Vo.setCategoryId(baseCategoryView.getCategory3Id());
                    category3Vo.setCategoryName(baseCategoryView.getCategory3Name());
                    category3Vo.setCategoryChild(null); // 这里我们最底下就是三级分类了，后面没有了
                    category2Child.add(category3Vo);

                }
                category2Vo.setCategoryChild(category2Child);
                category1Child.add(category2Vo);

            }

            category1Vo.setCategoryChild(category1Child);  // 给 1 级分类孩子赋值(就是二级分类)
            result.add(category1Vo);  // 将 1级分类放入到结果集中
        }


        return result;

    }
}
