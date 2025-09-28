package org.raibnowsea.cache.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;
import org.raibnowsea.cache.constant.CacheAbleConstant;
import org.raibnowsea.cache.service.CacheOpsService;
import org.raibnowsea.cache.utils.Jsons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 */
//@Service
public class CacheOpsServiceImpl implements CacheOpsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void saveDataToCache(String cacheKey, Object object) {
        // 1.将对象序列化成字符串
//        String s = JSONObject.toJSONString(object);
        // 2.将字符串存储到缓存中
//        redisTemplate.opsForValue().set(cacheKey,s);

        // 1.将对象序列化成字符串
        String resultStr = Jsons.objToStr(object);

        long ttl = CacheAbleConstant.HAS_DATA_TTL;
        List<String> allRegexRules = Jsons.getAllRegexRules();
        for (String allRegexRule : allRegexRules) {
            if (Jsons.isMath(resultStr, allRegexRule)) {
                ttl = CacheAbleConstant.NO_DATA_TTL;
            }
        }
        // 2.将字符串存储到缓存中
        redisTemplate.opsForValue().set(cacheKey, resultStr, ttl, TimeUnit.SECONDS);

    }

    @Override
    public <T> T getDataFromCache(String cacheKey, Class<T> clazz) {
        // 1.从缓存中获取数据
        // 2.将获取的数据反序列化成指定类型的对象
        // 3.返回指定类型的对象
        String resultStr = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isEmpty(resultStr)) {
            return null;
        }
        return Jsons.strToObj(resultStr, clazz);


    }

    @Override
    public <T> T getDataFromCache(String cacheKey, TypeReference<T> tTypeReference) {
        // 1.从缓存中获取数据
        // 2.将获取的数据反序列化成指定类型的对象
        // 3.返回指定类型的对象
        String resultStr = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isEmpty(resultStr)) {
            return null;
        }
        return Jsons.strToObj(resultStr, tTypeReference);
    }
}
