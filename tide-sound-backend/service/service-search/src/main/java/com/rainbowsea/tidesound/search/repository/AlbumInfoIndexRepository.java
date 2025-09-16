package com.rainbowsea.tidesound.search.repository;

import com.rainbowsea.tidesound.model.search.AlbumInfoIndex;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


/**
 * 操作专辑的 ES
 */
@Repository
public interface AlbumInfoIndexRepository extends CrudRepository<AlbumInfoIndex, Long> {
}
