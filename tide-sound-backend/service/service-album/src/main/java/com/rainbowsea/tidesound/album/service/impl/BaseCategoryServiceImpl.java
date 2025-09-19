package com.rainbowsea.tidesound.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.rainbowsea.tidesound.album.mapper.BaseAttributeMapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategory1Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategory2Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategory3Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategoryViewMapper;
import com.rainbowsea.tidesound.album.service.BaseCategoryService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.model.album.BaseAttribute;
import com.rainbowsea.tidesound.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.model.album.BaseCategory2;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.rainbowsea.tidesound.vo.category.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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



    // 操作数据表
    @Autowired
    private BaseAttributeMapper baseAttributeMapper;


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

    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {

        return baseAttributeMapper.findAttribute(category1Id);
    }

    @Override
    public BaseCategoryView getAlbumCategory(Long albumId) {

        return baseCategoryViewMapper.getAlbumCategory(albumId);
    }


    @Override
    public List<BaseCategory3> findTopBaseCategory3(Long c1Id) {

        // 1.根据一级分类id 查询该一级分类的二级分类集合

        LambdaQueryWrapper<BaseCategory2> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(BaseCategory2::getCategory1Id, c1Id);
        wrapper1.eq(BaseCategory2::getIsDeleted, 0);
        List<BaseCategory2> baseCategory2s = baseCategory2Mapper.selectList(wrapper1);
        if (CollectionUtils.isEmpty(baseCategory2s)) {
            throw new GuiguException(201, "该一级分类下不存在二级分类集合");
        }

        // 2.过滤二级分类的id
        List<Long> baseCategory2Ids = baseCategory2s.stream().map(BaseCategory2::getId).collect(Collectors.toList());


        // 3.查询三级分类
        LambdaQueryWrapper<BaseCategory3> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.in(BaseCategory3::getCategory2Id, baseCategory2Ids);
        wrapper2.eq(BaseCategory3::getIsTop, 1);  // 置顶展示的三级分类对象
        wrapper2.eq(BaseCategory3::getIsDeleted, 0);
        wrapper2.last("limit 7");
        List<BaseCategory3> category3s = baseCategory3Mapper.selectList(wrapper2);
        return category3s;
    }

    @Override
    public CategoryVo getBaseCategoryListByC1Id(Long c1Id) {


        // 1.根据一级分类id查询一级分类对象
        BaseCategory1 baseCategory1 = baseCategory1Mapper.selectById(c1Id);
        if (baseCategory1 == null) {
            throw new GuiguException(201, "该一级分类对象不存在");
        }


        CategoryVo categoryVo1 = new CategoryVo();  // 一级分类对象
        categoryVo1.setCategoryId(c1Id);
        categoryVo1.setCategoryName(baseCategory1.getName());

        // 2.根据一级分类id查询二级分类集合
        LambdaQueryWrapper<BaseCategory2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCategory2::getCategory1Id, c1Id);
        wrapper.eq(BaseCategory2::getIsDeleted, 0);
        List<BaseCategory2> baseCategory2s = baseCategory2Mapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(baseCategory2s)) {
            throw new GuiguException(201, "该一级分类下不存在二级分类集合");
        }


        List<CategoryVo> category1Child = new ArrayList<>();
        for (BaseCategory2 baseCategory2 : baseCategory2s) {
            CategoryVo category2Vo = new CategoryVo();  // 二级分类对象
            category2Vo.setCategoryId(baseCategory2.getId()); // 二级分类id
            category2Vo.setCategoryName(baseCategory2.getName()); // 二级分类的名字

            LambdaQueryWrapper<BaseCategory3> wrapper1 = new LambdaQueryWrapper<>();
            wrapper1.eq(BaseCategory3::getCategory2Id, baseCategory2.getId());
            wrapper1.eq(BaseCategory3::getIsDeleted, 0);
            List<BaseCategory3> category3s = baseCategory3Mapper.selectList(wrapper1);
            List<CategoryVo> category2Child = new ArrayList<>();
            for (BaseCategory3 category3 : category3s) {
                CategoryVo category3Vo = new CategoryVo();  // 三级分类对象
                category3Vo.setCategoryId(category3.getId());
                category3Vo.setCategoryName(category3.getName());
                category3Vo.setCategoryChild(Lists.newArrayList());
                category2Child.add(category3Vo);
            }
            category2Vo.setCategoryChild(category2Child); // 二级分类的孩子
            category1Child.add(category2Vo);
        }

        categoryVo1.setCategoryChild(category1Child); // 一级分类的孩子
        // 返回一级分类对象
        return categoryVo1;
    }

    @Override
    public List<Long> getAllCategory1Id() {

        List<BaseCategory1> baseCategory1s = baseCategory1Mapper.selectList(null);
        List<Long> c1Ids = baseCategory1s.stream().map(baseCategory1 -> baseCategory1.getId()).collect(Collectors.toList());
        return c1Ids;
    }

}
