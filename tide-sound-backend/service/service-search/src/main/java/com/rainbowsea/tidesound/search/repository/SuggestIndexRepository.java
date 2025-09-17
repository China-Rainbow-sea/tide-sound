package com.rainbowsea.tidesound.search.repository;

import com.rainbowsea.tidesound.model.search.SuggestIndex;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * ES操作实现搜索的时候，“智能提示词”功能
 * Description:
 */
@Repository
public interface SuggestIndexRepository extends CrudRepository<SuggestIndex, Long> {


}
