package com.rainbowsea.tidesound.search.service;

import com.rainbowsea.tidesound.query.search.AlbumIndexQuery;
import com.rainbowsea.tidesound.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SearchService {


    /**
     * 频道页数据展示
     *
     * @param c1Id
     * @return
     */
    List<Map<String, Object>> channel(Long c1Id);

    /**
     * 带条件的搜索
     *
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 智能提示词
     * @param input
     * @return
     */
    Set<String> completeSuggest(String input);
}
