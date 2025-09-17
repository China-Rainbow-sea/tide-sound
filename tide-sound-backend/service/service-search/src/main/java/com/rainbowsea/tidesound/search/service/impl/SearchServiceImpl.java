package com.rainbowsea.tidesound.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.model.search.AlbumInfoIndex;
import com.rainbowsea.tidesound.model.search.SuggestIndex;
import com.rainbowsea.tidesound.query.search.AlbumIndexQuery;
import com.rainbowsea.tidesound.search.service.SearchService;
import com.rainbowsea.tidesound.vo.search.AlbumInfoIndexVo;
import com.rainbowsea.tidesound.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {



    // OpenFeign 调用查询专辑下的3级分类信息列表
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;


    // 操作 ES 的客户端(原生API)
    @Autowired
    private ElasticsearchClient elasticsearchClient;













    // region  ===============  start 实现搜索的时候,"智能提示词" 功能 =======

    @SneakyThrows
    @Override
    public Set<String> completeSuggest(String input) {

        // 1.构建dsl语句
        SearchRequest searchRequest = this.buildCompletionDsl(input);


        // 2.开始查询
        SearchResponse<SuggestIndex> response = elasticsearchClient.search(searchRequest, SuggestIndex.class);


        // 3.解析数据
        Set<String> set = this.parseCompletionData(response);
        if (set.size() < 10) {

            // 补动作  开头的找不到10个 从包含中找几个  补够10个
            SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(sb -> sb.index("suggestinfo").query(qb -> qb.match(mqb -> mqb.field("title").query(input))), SuggestIndex.class);
            for (Hit<SuggestIndex> hit : searchResponse.hits().hits()) {
                SuggestIndex suggestIndex = hit.source();
                String title = suggestIndex.getTitle();
                set.add(title);
                if (set.size() >= 10)  // 够10个就 直接回去
                    break;

            }
        }
        // 4.返回
        return set;
    }


    /**
     * 构建 ES 搜索的时候，“智能提示词”  dsl语句
     * @param input
     * @return
     */
    private SearchRequest buildCompletionDsl(String input) {

        // 1.创建SearchRequestBuilder对象
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();

        // 2.创建SuggesterBuilder对象
        Suggester.Builder suggesterBuilder = new Suggester.Builder();


        // 3. 拼接提示词的 dsl
        suggesterBuilder
                .suggesters("suggestionKeyword", fsb -> fsb
                        .prefix(input)
                        .completion(csb -> csb.field("keyword")))

                .suggesters("suggestionKeywordPinyin", fsb -> fsb
                        .prefix(input)
                        .completion(csb -> csb.field("keywordPinyin")))

                .suggesters("suggestionKeywordSequence", fsb -> fsb
                        .prefix(input)
                        .completion(csb -> csb.field("keywordSequence")));

        // 4.得到 suggester
        Suggester suggester = suggesterBuilder.build();

        SearchRequest searchRequest = searchRequestBuilder.index("suggestinfo").suggest(suggester).build();

        System.out.println("提示词的dsl:" + searchRequest.toString());

        return searchRequest;

    }


    /**
     *  解析从 es中返回的数据（es查询的结果数据是JSON），从JSON格式中提取数据，封装到 Bean VO 当中返回给前端
     * @param response
     * @return
     */
    private Set<String> parseCompletionData(SearchResponse<SuggestIndex> response) {
        HashSet<String> result = new HashSet<>();

        // Map中有三对key,value
        // 第一对：key: suggestionKeyword  value:List<满足条件的内容>
        // 第二对：key: suggestionKeywordPinyin  value:List<满足条件的内容>
        // 第三对：key: suggestionKeywordSequence  value:List<满足条件的内容>
        Map<String, List<Suggestion<SuggestIndex>>> map = response.suggest();

        for (Map.Entry<String, List<Suggestion<SuggestIndex>>> stringListEntry : map.entrySet()) {   // 遍历三次

            List<Suggestion<SuggestIndex>> value = stringListEntry.getValue();//
            for (Suggestion<SuggestIndex> suggestIndexSuggestion : value) {   // 遍历一次
                List<CompletionSuggestOption<SuggestIndex>> options = suggestIndexSuggestion.completion().options();
                for (CompletionSuggestOption<SuggestIndex> option : options) {
                    SuggestIndex suggestIndex = option.source();
                    String title = suggestIndex.getTitle();// 去显示的
                    result.add(title);

                }
            }
        }
        return result;
    }



    // endregion   ===============  end  实现搜索的时候,"智能提示词" 功能 =======





    // region ==================一对 startES(频道页数据展示-带有条件的查询搜索) =============================





    @SneakyThrows
    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {


        // 1.构建带条件的查询dsl语句
        SearchRequest searchRequest = buildSearchDsl(albumIndexQuery);

        // 2.开始查询es,得到响应对象
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

        // 3.解析从es中返回的数据（es查询的结果数据是JSON），从JSON格式中提取数据，封装到 Bean Vo 当中返回给前端
        AlbumSearchResponseVo albumSearchResponseVo = parseSearchData(response);
        albumSearchResponseVo.setPageSize(albumIndexQuery.getPageSize());
        albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
        Long total = albumSearchResponseVo.getTotal();
        Long totalPages = total % albumIndexQuery.getPageSize() == 0 ? total / albumIndexQuery.getPageSize() : total / albumIndexQuery.getPageSize() + 1;
        albumSearchResponseVo.setTotalPages(totalPages);  // 总页数
        // 4.返回数据给前端
        return albumSearchResponseVo;
    }



    /**
     * 构建带条件的查询dsl语句
     *
     * @param albumIndexQuery
     * @return
     */
    private SearchRequest buildSearchDsl(AlbumIndexQuery albumIndexQuery) {
        // 1.构建最外层{}
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder();


        // 2.构建bool的查询bool查询的{}
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();


        // 3.用boolQueryBuilder进行查询（should以及must）

        // 3.1 判断关键字是否携带
        String keyword = albumIndexQuery.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            boolQueryBuilder.should(qb -> qb.match(mqb -> mqb.field("albumTitle").query(keyword)));
            boolQueryBuilder.should(qb -> qb.match(mqb -> mqb.field("albumIntro").query(keyword)));
            boolQueryBuilder.should(qb -> qb.match(mqb -> mqb.field("announcerName").query(keyword)));
        }

        // 3.2 判断分类id
        // a)一级分类id是否为空
        Long category1Id = albumIndexQuery.getCategory1Id();
        if (category1Id != null) {
            boolQueryBuilder.must(qb -> qb.term(tqb -> tqb.field("category1Id").value(category1Id)));
        }
        // a)二级分类id是否为空
        Long category2Id = albumIndexQuery.getCategory2Id();
        if (category2Id != null) {
            boolQueryBuilder.must(qb -> qb.term(tqb -> tqb.field("category2Id").value(category2Id)));
        }
        // a)三级分类id是否为空
        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category3Id != null) {
            boolQueryBuilder.must(qb -> qb.term(tqb -> tqb.field("category3Id").value(category3Id)));
        }

        // 3.3 判断标签是否携带了
        // String:"1:2" "2:4"
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (!CollectionUtils.isEmpty(attributeList)) {
            for (String attrAndValueId : attributeList) {
                String[] split = attrAndValueId.split(":");
                String attrId = split[0]; // 属性id
                String valueId = split[1]; // 属性值id获取到了
                // 创建NestedQueryBuilder对象
                NestedQuery.Builder builder = new NestedQuery.Builder();
                builder.path("attributeValueIndexList")
                        .query(qb -> qb
                                .bool(bqb -> bqb
                                        .must(mqb -> mqb
                                                .term(tqb -> tqb
                                                        .field("attributeValueIndexList.attributeId")
                                                        .value(attrId)))
                                        .must(mqb -> mqb
                                                .term(tqb -> tqb
                                                        .field("attributeValueIndexList.valueId")
                                                        .value(valueId)))));
                // 得到NestedQuery对象
                NestedQuery nestedQuery = builder.build();
                boolQueryBuilder.must(qb -> qb.nested(nestedQuery));
            }
        }

        // 得到boolQuery对象
        BoolQuery boolQuery = boolQueryBuilder.build();
        // boolQuery得到Query对象
        Query query = boolQuery._toQuery();

        searchBuilder.index("albuminfo").query(query);

        //============================基本条件的dsl语句编写完毕===============================


        // 3.4 判断分页
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        Integer from = (pageNo - 1) * pageSize;
        searchBuilder.from(from).size(pageSize);

        // 3.5 判断排序  (1:desc[asc]) (2:desc[asc])  (3:desc[asc])
        String order = albumIndexQuery.getOrder();

        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            String orderDimension = split[0]; // 维度
            String rule = split[1]; // 规则
            SortOrder sortOrder = rule.equals("desc") ? SortOrder.Desc : SortOrder.Asc;
            switch (orderDimension) {
                case "1":
                    // 处理热度值
                    searchBuilder.sort(sob -> sob.field(fsb -> fsb.field("hotScore").order(sortOrder)));
                    break;
                case "2":
                    // 处理购买量
                    searchBuilder.sort(sob -> sob.field(fsb -> fsb.field("buyStatNum").order(sortOrder)));
                    break;
                case "3":
                    searchBuilder.sort(sob -> sob.field(fsb -> fsb.field("createTime").order(sortOrder)));
                    // 处理发布时间
            }
        } else {  // 默认热度值降序排序
            searchBuilder.sort(sob -> sob.field(fsb -> fsb.field("hotScore").order(SortOrder.Desc)));
        }

        // 3.6 组合高亮

        searchBuilder.highlight(hb -> hb.fields("albumTitle", hfb -> hfb.preTags("<font style='color:red'>").postTags("</font>")));

        // 4.返回SearchRequest对象
        SearchRequest searchRequest = searchBuilder.build();
        System.out.println("搜索的dsl语句：" + searchRequest.toString());
        return searchRequest;

    }



    /**
     * 解析从 es中返回的数据（es查询的结果数据是JSON），从JSON格式中提取数据，封装到 Bean VO 当中返回给前端
     * @param response
     * @return
     */
    private AlbumSearchResponseVo parseSearchData(SearchResponse<AlbumInfoIndex> response) {

        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();

        TotalHits totalHits = response.hits().total();
        long value = totalHits.value();
        albumSearchResponseVo.setTotal(value); // 总专辑数

        List<AlbumInfoIndexVo> albumInfoIndexVoList = response.hits().hits().stream().map(albumInfoIndexHit -> {

            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            AlbumInfoIndex albumInfoIndex = albumInfoIndexHit.source();
            BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
            // 处理高亮
            Map<String, List<String>> highlight = albumInfoIndexHit.highlight();
            List<String> albumTitle = highlight.get("albumTitle");
            if (!CollectionUtils.isEmpty(albumTitle)) {
                String highlightAlbumTitle = albumTitle.get(0);
                albumInfoIndexVo.setAlbumTitle(highlightAlbumTitle); // 处理高亮
            }
            return albumInfoIndexVo;
        }).collect(Collectors.toList());

        albumSearchResponseVo.setList(albumInfoIndexVoList);   // 专辑集合
        return albumSearchResponseVo;
    }








    // endregion ========================================一对 end =============================================


    
    
    @SneakyThrows
    @Override
    public List<Map<String, Object>> channel(Long c1Id) {

        // 1.根据一级分类id 查询该一级分类下7个置顶的 3级 分类集合
        Result<List<BaseCategory3>> category3ListByC1Id = albumInfoFeignClient.getBaseCategory3ListByC1Id(c1Id);
        List<BaseCategory3> baseCategory3ListData = category3ListByC1Id.getData();
        if (CollectionUtils.isEmpty(baseCategory3ListData)) {
            throw new GuiguException(201, "远程调用分类信息失败");
        }

        // 2.过滤过去到三级分类的id集合 而且转成FieldValue类型（ES在查询的时候要用到这个类型）
        List<FieldValue> c3IdsFieldValue = baseCategory3ListData.stream().map(baseCategory3 -> {
            return FieldValue.of(baseCategory3.getId());
        }).collect(Collectors.toList());


        // 3.在将三级分类集合转成一个Map
        Map<Long, BaseCategory3> c3IdAndBaseCategory3 = baseCategory3ListData.stream().collect(Collectors.toMap(BaseCategory3::getId, v -> v));


        // 4.构建dsl语句
        SearchRequest searchRequest = this.buildChannelDsl(c3IdsFieldValue);

        // 5.查询es
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

        // 6.解析es的返回数据
        List<Map<String, Object>> result = this.parseChannelData(response, c3IdAndBaseCategory3);

        return result;
    }




    /**
     * 构建dsl语句
     * 查询当前一级分类下的三级分类集合中每一个三级分类对象中的 6个热度值高的专辑
     *
     * @param c3IdsFieldValue
     * @return
     */
    private SearchRequest buildChannelDsl(List<FieldValue> c3IdsFieldValue) {


        // 1.构建SearchRequestBuilder对象
        SearchRequest.Builder builder = new SearchRequest.Builder();
        // 2.构建查询
        builder.index("albuminfo")
                .query(b -> b
                        .terms(tqb -> tqb
                                .field("category3Id")
                                .terms(tqfb -> tqfb
                                        .value(c3IdsFieldValue))))
                .aggregations("category3IdAgg", ab -> ab
                        .terms(tab -> tab
                                .field("category3Id")
                                .size(c3IdsFieldValue.size()))
                        .aggregations("topHitHotScoreAgg", sab -> sab
                                .topHits(thab -> thab
                                        .sort(sob -> sob
                                                .field(fsb -> fsb
                                                        .field("hotScore")
                                                        .order(SortOrder.Desc)))
                                        .size(6))));
        SearchRequest searchRequest = builder.build();
        System.out.println("频道页的dsl:" + searchRequest.toString());
        // 3.返回SearchRequest对象
        return searchRequest;
    }


    /**
     * 解析es的返回数据
     *
     * @param response
     * @param c3IdAndBaseCategory3
     * @return
     */

    private List<Map<String, Object>> parseChannelData(SearchResponse<AlbumInfoIndex> response, Map<Long, BaseCategory3> c3IdAndBaseCategory3) {

        ArrayList<Map<String, Object>> result = new ArrayList<>();


        // 1.得到category3IdAgg聚合对象
        Aggregate category3IdAgg = response.aggregations().get("category3IdAgg");

        // 2.得到Aggregate具体的类型
        LongTermsAggregate longTermsAggregate = category3IdAgg.lterms();

        // 3.得到父聚合的桶
        List<LongTermsBucket> buckets = longTermsAggregate.buckets().array();

        for (LongTermsBucket bucket : buckets) {
            // 得到子聚合
            Aggregate topHitHotscoreAgg = bucket.aggregations().get("topHitHotScoreAgg");
            // 把子聚合转为精准的类型
            TopHitsAggregate topHitsAggregate = topHitHotscoreAgg.topHits();
            ArrayList<AlbumInfoIndex> albumInfoIndices = new ArrayList<>();
            for (Hit<JsonData> hit : topHitsAggregate.hits().hits()) {
                JsonData source = hit.source();
                AlbumInfoIndex albumInfoIndex = JSONObject.parseObject(source.toString(), AlbumInfoIndex.class);
                albumInfoIndices.add(albumInfoIndex);
            }

            HashMap<String, Object> map = new HashMap<>();
            long category3Id = bucket.key();
            map.put("baseCategory3", c3IdAndBaseCategory3.get(category3Id));  // 当前三级分类对象
            map.put("list", albumInfoIndices);  // 当前三级分类下热度值高的前6个专辑集合
            result.add(map);
        }
        return result;
    }


}
