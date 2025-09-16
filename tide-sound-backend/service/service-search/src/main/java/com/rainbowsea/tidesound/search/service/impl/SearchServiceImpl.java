package com.rainbowsea.tidesound.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSONObject;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.BaseCategory3;
import com.rainbowsea.tidesound.model.search.AlbumInfoIndex;
import com.rainbowsea.tidesound.search.service.SearchService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        SearchRequest searchRequest = buildChannelDsl(c3IdsFieldValue);

        // 5.查询es
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

        // 6.解析es的返回数据
        List<Map<String, Object>> result = parseChannelData(response, c3IdAndBaseCategory3);

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
