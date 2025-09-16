package com.rainbowsea.tidesound.search.service;

import java.util.List;
import java.util.Map;

public interface SearchService {


    /**
     * 频道页数据展示
     *
     * @param c1Id
     * @return
     */
    List<Map<String, Object>> channel(Long c1Id);
}
