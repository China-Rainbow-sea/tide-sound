package org.raibnowsea.cache.service;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 *
 * 对缓存进行读写操作的API封装
 */
public interface CacheOpsService {

    /**
     * 将数据写入到缓存中(写操作)
     * cacheKey:将数据存储到缓存中用到的key
     * Object object：要保存进缓存的数据对象
     */

    public void saveDataToCache(String cacheKey, Object object);


    /**
     * 不带泛型
     * 从缓存中将数据读取出来（读操作）
     * cacheKey:从缓存获取数据要用到的key
     * clazz：要从缓存中反序列化的类型
     */

    public <T> T getDataFromCache(String cacheKey, Class<T> clazz);


    /**
     * 带泛型
     * 从缓存中将数据读取出来（读操作）
     * cacheKey:从缓存获取数据要用到的key
     * clazz：要从缓存中反序列化的类型
     */

    public <T> T getDataFromCache(String cacheKey, TypeReference<T> tTypeReference);


}
