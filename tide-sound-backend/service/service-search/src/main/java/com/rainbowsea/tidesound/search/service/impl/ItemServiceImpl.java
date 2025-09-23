package com.rainbowsea.tidesound.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.javax.annotation.Nullable;
import com.google.common.collect.Lists;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.constant.RedisConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.PinYinUtils;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.rainbowsea.tidesound.model.search.AlbumInfoIndex;
import com.rainbowsea.tidesound.model.search.AttributeValueIndex;
import com.rainbowsea.tidesound.model.search.SuggestIndex;
import com.rainbowsea.tidesound.search.executor.ExpireThreadExecutor;
import com.rainbowsea.tidesound.search.repository.AlbumInfoIndexRepository;
import com.rainbowsea.tidesound.search.repository.SuggestIndexRepository;
import com.rainbowsea.tidesound.search.service.ItemService;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.rainbowsea.tidesound.vo.album.AlbumStatVo;
import com.rainbowsea.tidesound.vo.search.AlbumInfoIndexVo;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ItemServiceImpl implements ItemService {


    // 定义操作 ES 实现类
    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    // 定义操作 ES
    @Autowired
    private SuggestIndexRepository suggestIndexRepository;


    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    // java.util.concurrent 包下的
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ElasticsearchClient elasticsearchClient;


    // 标记同一线程，只能释放自己的该线程创建的锁，防止误删锁
    ThreadLocal<String> reentrantLockTokenThreadLocal = new ThreadLocal<>();


    // 这个线程池，默认 4 个线程(因为我们这里线程配合只需要 4 个线程 减减即可)
    ExecutorService executorService = Executors.newFixedThreadPool(4);


    @SneakyThrows
    @Override
    public void albumOnSale(Long albumId) {

        CountDownLatch countDownLatch = new CountDownLatch(4);  // 其它线程干活的线程数

        // 1.创建文档对象
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        Long startTime = System.currentTimeMillis();

        Future<Long> future = executorService.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                // 2.1 远程查询专辑基本信息
                Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoAndAttrValue(albumId);
                AlbumInfo albumInfoData = albumInfoResult.getData();
                if (albumInfoData == null) {
                    throw new GuiguException(201, "远程调用专辑微服务获取专辑信息失败");
                }

                albumInfoIndex.setId(albumInfoData.getId());  // 专辑id
                albumInfoIndex.setAlbumTitle(albumInfoData.getAlbumTitle());   // 专辑标题
                albumInfoIndex.setAlbumIntro(albumInfoData.getAlbumIntro()); // 专辑简介
                albumInfoIndex.setCoverUrl(albumInfoData.getCoverUrl());  // 专辑封面
                albumInfoIndex.setIncludeTrackCount(albumInfoData.getIncludeTrackCount()); // 专辑包含的声音集数
                albumInfoIndex.setIsFinished(albumInfoData.getIsFinished().toString()); // 专辑是否完结
                albumInfoIndex.setPayType(albumInfoData.getPayType()); // 专辑付费类型（免费 vip免费  付费）
                albumInfoIndex.setCreateTime(new Date());  // 专辑保存到es的时间

                List<AttributeValueIndex> attributeValueIndexs = albumInfoData.getAlbumAttributeValueVoList().stream().map(albumAttributeValue -> {
                    AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                    attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                    attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                    return attributeValueIndex;
                }).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(Lists.newArrayList(attributeValueIndexs));   // 专辑的标签

                countDownLatch.countDown();
                return albumInfoData.getUserId();
            }
        });

        executorService.execute(new Runnable() {

            // 2.2 远程查询主播信息
            @Override
            public void run() {
                Long userId = null;
                try {
                    userId = future.get();   // 当前线程会阻塞
                    Result<UserInfoVo> albumInfoVoResult = userInfoFeignClient.getUserInfo(userId);
                    UserInfoVo userInfoVoData = albumInfoVoResult.getData();
                    Assert.notNull(userInfoVoData, "远程调用用户微服务获取用户信息失败");
                    albumInfoIndex.setAnnouncerName(userInfoVoData.getNickname()); // 专辑对应的主播名字
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }

            }
        });


        executorService.execute(new Runnable() {
            // 2.3 远程查询分类信息
            @Override
            public void run() {
                Result<BaseCategoryView> baseCategoryViewResult = albumInfoFeignClient.getAlbumCategory(albumId);
                BaseCategoryView baseCategoryViewData = baseCategoryViewResult.getData();
                Assert.notNull(baseCategoryViewData, "远程调用专辑微服务获取分类信息失败");

                albumInfoIndex.setCategory1Id(baseCategoryViewData.getCategory1Id()); // 专辑一级分类id
                albumInfoIndex.setCategory2Id(baseCategoryViewData.getCategory2Id()); // 专辑二级分类id
                albumInfoIndex.setCategory3Id(baseCategoryViewData.getCategory3Id()); // 专辑二级分类id
                countDownLatch.countDown();
            }
        });


        executorService.execute(new Runnable() {
            // 2.4 远程查询统计信息
            @Override
            public void run() {
                Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStat(albumId);
                AlbumStatVo albumStatVoData = albumStatVoResult.getData();
                if (albumStatVoData == null) {
                    throw new GuiguException(201, "远程调用专辑微服务获取专辑分类信息失败");
                }

                Integer commentStatNum = albumStatVoData.getCommentStatNum();
                Integer subscribeStatNum = albumStatVoData.getSubscribeStatNum();
                Integer playStatNum = albumStatVoData.getPlayStatNum();
                Integer buyStatNum = albumStatVoData.getBuyStatNum();

                albumInfoIndex.setPlayStatNum(playStatNum);  // 专辑的播放量
                albumInfoIndex.setSubscribeStatNum(subscribeStatNum); // 专辑的订阅量
                albumInfoIndex.setBuyStatNum(buyStatNum); // 专辑的购买量
                albumInfoIndex.setCommentStatNum(commentStatNum); // 专辑的评论数
                Double hotScore = new Random().nextDouble(); // 测试环境用
                albumInfoIndex.setHotScore(hotScore); // 专辑热度值
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
        // 3.将文档对象存储到es中
        Long endTime = System.currentTimeMillis();

        log.info("专辑:{}上架到es耗时：{}ms", albumId, endTime - startTime);
        albumInfoIndexRepository.save(albumInfoIndex);


        // 像suggestInfo索引库中保存数据

        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(albumInfoIndex.getId().toString());
        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        // 专辑标题：我喜欢纯音乐

        suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));  // 我喜欢纯音乐
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));  //  woxihuhancunyinyue
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())})); // wxhcyy
        suggestIndexRepository.save(suggestIndex);
    }

    /**
     * 异步：线程池：（countdownlatch使用）第一次259ms   后面的平均在30ms作用
     *
     * @param albumId
     */
    //@SneakyThrows
    //@Override
    //public void albumOnSale(Long albumId) {
    //
    //    CountDownLatch countDownLatch = new CountDownLatch(4);  // 其它线程干活的线程数
    //
    //    // 1.创建文档对象
    //    AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
    //
    //    Long startTime = System.currentTimeMillis();
    //
    //    Future<Long> future = executorService.submit(new Callable<Long>() {
    //        @Override
    //        public Long call() throws Exception {
    //            // 2.1 远程查询专辑基本信息
    //            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoAndAttrValue(albumId);
    //            AlbumInfo albumInfoData = albumInfoResult.getData();
    //            if (albumInfoData == null) {
    //                throw new GuiguException(201, "远程调用专辑微服务获取专辑信息失败");
    //            }
    //
    //            albumInfoIndex.setId(albumInfoData.getId());  // 专辑id
    //            albumInfoIndex.setAlbumTitle(albumInfoData.getAlbumTitle());   // 专辑标题
    //            albumInfoIndex.setAlbumIntro(albumInfoData.getAlbumIntro()); // 专辑简介
    //            albumInfoIndex.setCoverUrl(albumInfoData.getCoverUrl());  // 专辑封面
    //            albumInfoIndex.setIncludeTrackCount(albumInfoData.getIncludeTrackCount()); // 专辑包含的声音集数
    //            albumInfoIndex.setIsFinished(albumInfoData.getIsFinished().toString()); // 专辑是否完结
    //            albumInfoIndex.setPayType(albumInfoData.getPayType()); // 专辑付费类型（免费 vip免费  付费）
    //            albumInfoIndex.setCreateTime(new Date());  // 专辑保存到es的时间
    //
    //            List<AttributeValueIndex> attributeValueIndexs = albumInfoData.getAlbumAttributeValueVoList().stream().map(albumAttributeValue -> {
    //                AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
    //                attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
    //                attributeValueIndex.setValueId(albumAttributeValue.getValueId());
    //                return attributeValueIndex;
    //            }).collect(Collectors.toList());
    //            albumInfoIndex.setAttributeValueIndexList(Lists.newArrayList(attributeValueIndexs));   // 专辑的标签
    //
    //            countDownLatch.countDown();
    //            return albumInfoData.getUserId();
    //        }
    //    });
    //
    //    executorService.execute(new Runnable() {
    //
    //        // 2.2 远程查询主播信息
    //        @Override
    //        public void run() {
    //            Long userId = null;
    //            try {
    //                userId = future.get();   // 当前线程会阻塞
    //                Result<UserInfoVo> albumInfoVoResult = userInfoFeignClient.getUserInfo(userId);
    //                UserInfoVo userInfoVoData = albumInfoVoResult.getData();
    //                org.springframework.util.Assert.notNull(userInfoVoData, "远程调用用户微服务获取用户信息失败");
    //                albumInfoIndex.setAnnouncerName(userInfoVoData.getNickname()); // 专辑对应的主播名字
    //            } catch (Exception e) {
    //                throw new RuntimeException(e);
    //            } finally {
    //                countDownLatch.countDown();
    //            }
    //
    //        }
    //    });
    //
    //
    //    executorService.execute(new Runnable() {
    //        // 2.3 远程查询分类信息
    //        @Override
    //        public void run() {
    //            Result<BaseCategoryView> baseCategoryViewResult = albumInfoFeignClient.getAlbumCategory(albumId);
    //            BaseCategoryView baseCategoryViewData = baseCategoryViewResult.getData();
    //            Assert.notNull(baseCategoryViewData, "远程调用专辑微服务获取分类信息失败");
    //
    //            albumInfoIndex.setCategory1Id(baseCategoryViewData.getCategory1Id()); // 专辑一级分类id
    //            albumInfoIndex.setCategory2Id(baseCategoryViewData.getCategory2Id()); // 专辑二级分类id
    //            albumInfoIndex.setCategory3Id(baseCategoryViewData.getCategory3Id()); // 专辑二级分类id
    //            countDownLatch.countDown();
    //        }
    //    });
    //
    //
    //    executorService.execute(new Runnable() {
    //        // 2.4 远程查询统计信息
    //        @Override
    //        public void run() {
    //            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStat(albumId);
    //            AlbumStatVo albumStatVoData = albumStatVoResult.getData();
    //            if (albumStatVoData == null) {
    //                throw new GuiguException(201, "远程调用专辑微服务获取专辑分类信息失败");
    //            }
    //
    //            Integer commentStatNum = albumStatVoData.getCommentStatNum();
    //            Integer subscribeStatNum = albumStatVoData.getSubscribeStatNum();
    //            Integer playStatNum = albumStatVoData.getPlayStatNum();
    //            Integer buyStatNum = albumStatVoData.getBuyStatNum();
    //
    //            albumInfoIndex.setPlayStatNum(playStatNum);  // 专辑的播放量
    //            albumInfoIndex.setSubscribeStatNum(subscribeStatNum); // 专辑的订阅量
    //            albumInfoIndex.setBuyStatNum(buyStatNum); // 专辑的购买量
    //            albumInfoIndex.setCommentStatNum(commentStatNum); // 专辑的评论数
    //            Double hotScore = new Random().nextDouble(); // 测试环境用
    //            albumInfoIndex.setHotScore(hotScore); // 专辑热度值
    //            countDownLatch.countDown();
    //        }
    //    });
    //
    //    countDownLatch.await();
    //    // 3.将文档对象存储到es中
    //    Long endTime = System.currentTimeMillis();
    //
    //    log.info("专辑:{}上架到es耗时：{}ms", albumId, endTime - startTime);
    //    albumInfoIndexRepository.save(albumInfoIndex);
    //
    //
    //
    //    // 像 suggestInfo 索引库中保存数据
    //
    //    SuggestIndex suggestIndex = new SuggestIndex();
    //    suggestIndex.setId(albumInfoIndex.getId().toString());
    //    suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
    //
    //    // 专辑标题：我喜欢纯音乐
    //    suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));  // 我喜欢纯音乐
    //    suggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));  //  woxihuhancunyinyue
    //    suggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())})); // wxhcyy
    //    suggestIndexRepository.save(suggestIndex);
    //
    //
    //}
    @Override
    public void albumOffSale(Long albumId) {

        try {
            albumInfoIndexRepository.deleteById(albumId);
            log.info("专辑下架成功");
        } catch (Exception e) {
            log.error("专辑下架失败");
        }
    }


    @Override
    public void batchAlbumOffSale() {
        try {
            albumInfoIndexRepository.deleteAll();
            log.info("专辑批量下架成功");
        } catch (Exception e) {
            log.error("专辑批量下架失败");
        }

    }


    // region  ---------------- 获取专辑优化: start

    /**
     * 1、异步优化
     *
     * @param albumId
     * @return
     */
    @Override
    public Map<String, Object> getAlbumInfo(Long albumId) {

        // v4:V4 Redis分布式锁+自旋可重入+双缓存查询的缓存使用 + 分布式锁续期
        return getDistroCacheAndLockFinallyVersion(albumId);

        //v3:解决锁的误删问题
        //return getDistroCacheAndLockV3(albumId);

        // v2: 解决分布式锁，极端情况下，断电分布式锁没有被及时释放掉,导致死锁
        //return getDistroCacheAndLockV2(albumId);

        // v1:  分布式缓存Redis+Redis版本的分布式锁 解决缓存击穿问题。
        //return opsDistroCacheAndLockV1(albumId);
    }



    /**
     * finally version:
     * Redis分布式锁+自旋可重入+双缓存查询的缓存使用。
     *
     * @param albumId
     * @return // 续期：
     * 要做的： 只要抢到锁的线程没有把自己的活干完，这个抢到锁的线程对应的这个锁key就不能释放掉。只有等抢到锁的线程把活干完了或者干活期间出异常，才让在这个锁过期。
     * <p>
     * // 活没干完----给锁key续期
     * // 活干完或者干期间出异常---不用在给锁key续期
     * 注意：只有抢到锁，才续期，没抢到就别续。
     * <p>
     * // 启动一个线程--->负责完成续期任务。
     * <p>
     * // 方案1：new Thread线程 让这个线程做续期任务（一直做） 并且让这个线程作为守护线程。 最后在利用Thread的中断机制，完成对续期线程的取消。
     * // 方案2：用线程池完成续期。【定时或者延时任务的线程池实现】
     * <p>
     * // 续期：每隔多久 在让redis中的锁key的时间是一个新值。每隔10s钟给我将Redis中的锁key设置为30s.
     */

    private Map getDistroCacheAndLockFinallyVersion(Long albumId) {

        // 1.定义变量
        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;
        String lockKey = RedisConstant.ALBUM_LOCK_SUFFIX + albumId;
        String token = "";
        Boolean acquireLockFlag = false;

        // 2.查询分布式缓存
        String resultFromCache = redisTemplate.opsForValue().get(cacheKey);

        // 3.判断缓存是否存在
        if (!StringUtils.isEmpty(resultFromCache)) {
            return JSONObject.parseObject(resultFromCache, Map.class);
        }

        // 4.缓存未命中 准备查询数据库
        // 4.1 从ThreadLocal中获取令牌值（解决递归的线程进来）
        String s = reentrantLockTokenThreadLocal.get();
        // 4.2 如果是递归进来的线程
        if (!StringUtils.isEmpty(s)) {
            token = s;
            acquireLockFlag = true;
        } else {
            // 4.3 第一次进来
            token = UUID.randomUUID().toString().replace("-", "");
            // 4.4 加分布式锁
            acquireLockFlag = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 30, TimeUnit.SECONDS);//  服务端给锁一个过期时间  避免死锁发生
        }
        // 5.加分布式锁成功
        if (acquireLockFlag) {
            // 开始续期
            ExpireThreadExecutor expireThreadExecutor = new ExpireThreadExecutor(redisTemplate, albumId);
            expireThreadExecutor.renewal(30l, TimeUnit.SECONDS);

            Map<String, Object> albumInfoFromDb;
            try {
                // 5.1 回源查询数据库
                albumInfoFromDb = getAlbumInfoFromDb(albumId);
//                 5.2 将数据库查询的数据同步给缓存Redis
                redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDb));
            } finally {
                // 5.3 释放锁
                // 判断是不是自己加的锁 是自己加的锁 才删除 否则不能删除
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(luaScript, Long.class), Arrays.asList(lockKey), token);
                if (execute == 0) {
                    log.error("释放锁失败");
                } else {
                    log.info("释放锁成功");
                }
                // 5.4 从ThreadLocal移除令牌
                reentrantLockTokenThreadLocal.remove();// 防止内存泄漏问题。用完一定要给他删除掉。

                // 5.5 结束续期任务
                expireThreadExecutor.cancelRenewal();
            }
            // 5.6 返回数据给前端
            return albumInfoFromDb;
        } else {
            // 6.加分布式锁失败
            // 6.1.等同步时间
            try {
                Thread.sleep(200);// 一定要压测得到准确值
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 6.2 查询缓存--正常99%的情况下200ms之后的缓存一定是有数据。所以直接返回出去即可
            String firstCacheStr = redisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.isEmpty(firstCacheStr)) {
                return JSONObject.parseObject(firstCacheStr, Map.class);
            }

            // 6.3 解决1%极端情况（抢到锁的线程在将数据库中的数据同步到缓存的时候出现了问题，导致缓存没有）
            while (true) {// 通过监控工具排查定位cpu飙升原因  // 兜底---自旋就是while[自旋+可重复入]
                // 6.4 查询缓存的作用：主要是为了解决，递归进去的线程将数据同步到缓存之后，其它线程还要抢锁。
                String doubleCacheStr = redisTemplate.opsForValue().get(cacheKey);
                // 6.5 如果有，要在while..true抢锁的线程不用在抢锁。直接将递归进去的线程放到缓存中的数据返回即可。
                if (!StringUtils.isEmpty(doubleCacheStr)) {
                    return JSONObject.parseObject(doubleCacheStr);
                }
                // 6.6 抢锁
                Boolean acquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 30, TimeUnit.SECONDS);
                if (acquireLock) {
                    reentrantLockTokenThreadLocal.set(token); //将该线程加锁的令牌存放到ThrealLocal.主要解决递归进去的线程不再加锁。【保证可重入锁】
                    break; // 退出循环
                }
            }
            // 7. 重新递归进去查询数据库
            return getAlbumInfo(albumId);
        }
    }

    /**
     * v3:解决锁的误删问题
     * <p>
     * 解决：每个线程加锁都给一个锁标识 然后在释放锁的时候  先判断一下锁是不是自己加的 如果是则删除 如果不是 则不能删除。
     * <p>
     * 问题：判断锁和释放锁不是原子操作。
     * 解决办法：自定定义lua脚本的表达式来讲判断锁和释放锁做成原子操作。从而保证锁的误删真正被解决。
     *
     * @param albumId
     * @return
     */
    @org.jetbrains.annotations.Nullable
    private Map getDistroCacheAndLockV3(Long albumId) {
        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;

        String lockKey = RedisConstant.ALBUM_LOCK_SUFFIX + albumId;

        String token = UUID.randomUUID().toString().replace("-", "");
        // 1.查询分布式缓存
        String resultFromCache = redisTemplate.opsForValue().get(cacheKey);

        // 2.判断缓存是否存在
        if (!StringUtils.isEmpty(resultFromCache)) {
            return JSONObject.parseObject(resultFromCache, Map.class);
        }

        // 3.缓存未命中 回源查询数据库
        // 3.1 加分布式锁
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 30, TimeUnit.SECONDS);//  服务端给锁一个过期时间  避免死锁发生
        if (aBoolean) {
            Map<String, Object> albumInfoFromDb;
            try {
                // 3.2 回源查询数据库
                albumInfoFromDb = getAlbumInfoFromDb(albumId);
                // 3.3 将数据库查询的数据同步给缓存Redis
                redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDb));
            } finally {
                // 3.4 释放锁
                // 判断是不是自己加的锁 是自己加的锁 才删除 否则不能删除
                String lockValueFromCache = redisTemplate.opsForValue().get(lockKey);
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(luaScript, Long.class), Arrays.asList(lockKey), token);
                if (execute == 0) {
                    log.error("释放锁失败");
                } else {
                    log.info("释放锁成功");
                }
                if (token.equals(lockValueFromCache)) {
                    redisTemplate.delete(lockKey);  // 删除 【别人还没有加锁 可以删除】
                }
            }


            // 3.5 返回数据给前端
            return albumInfoFromDb;
        } else {
            // 4.等同步时间
            try {
                Thread.sleep(200);// 一定要压测得到准确值
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String resultFromCacheStr = redisTemplate.opsForValue().get(cacheKey);
            return JSONObject.parseObject(resultFromCacheStr, Map.class);

        }
    }




    /**
     * v2:解决在极端情况下，抢到锁的线程刚执行业务，断电。导致Redis中的锁key没有被释放，那么就会导致死锁发生
     * 解决办法：
     * 1、客户端主动删除锁：      redisTemplate.delete(lockKey);
     * 2、Redis服务端给锁key过期时间 redisTemplate.expire(lockKey, 30, TimeUnit.SECONDS);
     * <p>
     * v2:问题：
     * 在极端情况下，抢到锁的线程，正准备给锁key设置过期时间的时候断电了，那么还是会出现死锁问题。
     * 解决办法：将加锁和给锁设置过期时间的两步动作做成一个原子操作。 redisTemplate.opsForValue().setIfAbsent(lockKey, "lock", 30, TimeUnit.SECONDS)
     * 底层实现：redis将设置key和给这个key设置过期时间用一个lua脚本包装起来 然后用一个Redis连接执行这个lua脚本 从而保证这两个动作的原子性。
     *
     * @param albumId
     * @return
     */
    @Nullable
    private Map getDistroCacheAndLockV2(Long albumId) {
        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;

        String lockKey = RedisConstant.ALBUM_LOCK_SUFFIX + albumId;
        // 1.查询分布式缓存
        String resultFromCache = redisTemplate.opsForValue().get(cacheKey);

        // 2.判断缓存是否存在
        if (!StringUtils.isEmpty(resultFromCache)) {
            return JSONObject.parseObject(resultFromCache, Map.class);
        }

        // 3.缓存未命中 回源查询数据库
        // 3.1 加分布式锁
        //  服务端给锁一个过期时间  避免死锁发生
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, "lock", 30, TimeUnit.SECONDS);
        if (aBoolean) {
            Map<String, Object> albumInfoFromDb;

            try {
                // 3.2 回源查询数据库
                albumInfoFromDb = getAlbumInfoFromDb(albumId);

                // 3.3 将数据库查询的数据同步给缓存Redis
                redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDb));


            } finally {
                // 3.4 释放锁
                redisTemplate.delete(lockKey);
            }


            // 3.5 返回数据给前端
            return albumInfoFromDb;
        } else {
            // 4.等同步时间
            try {
                Thread.sleep(200);// 一定要压测得到准确值
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String resultFromCacheStr = redisTemplate.opsForValue().get(cacheKey);
            return JSONObject.parseObject(resultFromCacheStr, Map.class);

        }
    }



    /**
     * 分布式缓存Redis+Redis版本的分布式锁 解决缓存击穿问题。
     * <p>
     * v1版本
     *
     * @param albumId
     * @return
     */

    @Nullable
    private Map opsDistroCacheAndLockV1(Long albumId) {

        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;
        String lockKey = RedisConstant.ALBUM_LOCK_SUFFIX + albumId;
        // 1.查询分布式缓存
        String resultFromCache = redisTemplate.opsForValue().get(cacheKey);

        // 2.判断缓存是否存在
        if (!StringUtils.isEmpty(resultFromCache)) {
            return JSONObject.parseObject(resultFromCache, Map.class);
        }

        // 3.缓存未命中 回源查询数据库
        // 3.1 加分布式锁
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, "lock");

        if (aBoolean) {
            Map<String, Object> albumInfoFromDb;

            // 3.2 回源查询数据库
            albumInfoFromDb = getAlbumInfoFromDb(albumId);

            // 3.3 将数据库查询的数据同步给缓存Redis
            redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDb));

            // 3.4 释放锁
            redisTemplate.delete(lockKey);


            // 3.5 返回数据给前端
            return albumInfoFromDb;
        }
        else {
            // 4.等同步时间
            try {
                Thread.sleep(200);// 一定要压测得到准确值
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String resultFromCacheStr = redisTemplate.opsForValue().get(cacheKey);
            return JSONObject.parseObject(resultFromCacheStr, Map.class);

        }

    }

    /**
     * 回源查询数据库,同步数据到 ES 当中
     *
     * @param albumId
     * @return
     */
    @NotNull
    private Map<String, Object> getAlbumInfoFromDb(Long albumId) {
        // 1.创建Map对象
        Map<String, Object> map = new HashMap<>();


        CompletableFuture<Void> albumStatCompletableFuture = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println("执行查询专辑统计任务用的线程：" + Thread.currentThread().getName());
                // 1. 专辑的统计信息
                Result<AlbumStatVo> albumStatResult = albumInfoFeignClient.getAlbumStat(albumId);
                AlbumStatVo albumStatVoData = albumStatResult.getData();
                if (albumStatVoData == null) {
                    throw new GuiguException(201, "远程查询专辑微服务获取专辑统计信息失败");
                }
                map.put("albumStatVo", albumStatVoData);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> viewCompletableFuture = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println("执行查询专辑分类任务用的线程：" + Thread.currentThread().getName());
                // 2. 专辑的分类（分类的名字）`
                Result<BaseCategoryView> albumCategoryResult = albumInfoFeignClient.getAlbumCategory(albumId);
                BaseCategoryView baseCategoryViewData = albumCategoryResult.getData();
                if (baseCategoryViewData == null) {
                    throw new GuiguException(201, "远程查询专辑微服务获取专辑分类信息失败");
                }
                map.put("baseCategoryView", baseCategoryViewData);
            }
        }, threadPoolExecutor);


        CompletableFuture<Long> albumInfoCompletableFuture = CompletableFuture.supplyAsync(new Supplier<Long>() {
            @Override
            public Long get() {
                System.out.println("执行查询专辑基本信息任务用的线程：" + Thread.currentThread().getName());
                // 3. 专辑基本数据
                Result<AlbumInfo> albumInfoAndAttrValueResult = albumInfoFeignClient.getAlbumInfoAndAttrValue(albumId);
                AlbumInfo albumInfoData = albumInfoAndAttrValueResult.getData();
                if (albumInfoData == null) {
                    throw new GuiguException(201, "远程查询专辑微服务获取专辑基本信息失败");
                }
                map.put("albumInfo", albumInfoData);

                return albumInfoData.getUserId();
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(new Consumer<Long>() {
            @Override
            public void accept(Long userId) {
                System.out.println("执行查询专辑对应主播任务用的线程：" + Thread.currentThread().getName());
                // 4.查询专辑对应的主播信息
                Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfo(userId);
                UserInfoVo userInfoResultData = userInfoResult.getData();
                if (userInfoResultData == null) {
                    throw new GuiguException(201, "远程查询专辑微服务获取专辑基本信息失败");
                }
                map.put("announcer", userInfoResultData);

            }
        }, threadPoolExecutor);

        CompletableFuture.allOf(albumStatCompletableFuture, viewCompletableFuture, albumInfoCompletableFuture, userCompletableFuture).join();

        return map;
    }


    // endregion  ---------------- 获取专辑优化: end

    @SneakyThrows
    @Override
    public void preRankingToCache() {

        // 1.查询全平台的一级分类id
        Result<List<Long>> c1IdsResult = albumInfoFeignClient.getAllCategory1Id();
        List<Long> c1IdData = c1IdsResult.getData();
        if (CollectionUtils.isEmpty(c1IdData)) {
            throw new GuiguException(201, "远程查询专辑微服务获取一级分类id失败");
        }


        for (Long c1Id : c1IdData) {

            String[] fiveDimension = {"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
            for (String dimension : fiveDimension) {
                SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(srb -> srb
                        .index("albuminfo")
                        .query(qb -> qb
                                .term(tqb -> tqb
                                        .field("category1Id")
                                        .value(c1Id)))
                        .sort(sob -> sob.field(fsb -> fsb.field(dimension)))
                        .size(10), AlbumInfoIndex.class);

                List<AlbumInfoIndex> albumInfoIndices = new ArrayList<>();

                for (Hit<AlbumInfoIndex> hit : response.hits().hits()) {
                    AlbumInfoIndex albumInfoIndex = hit.source();
                    albumInfoIndices.add(albumInfoIndex);
                }

                // Redis：String  set  zset  hash(大key  小key )  list

                String bigKey = RedisConstant.RANKING_KEY_PREFIX + c1Id;
                redisTemplate.opsForHash().put(bigKey, dimension, JSONObject.toJSONString(albumInfoIndices));
            }
        }

    }


    @Override
    public List<AlbumInfoIndexVo> findRankingList(Long c1Id, String dimension) {


        String bigKey = RedisConstant.RANKING_KEY_PREFIX + c1Id;
        String albumInfoIndexList = (String) redisTemplate.opsForHash().get(bigKey, dimension);
        if (StringUtils.isEmpty(albumInfoIndexList)) {
            throw new GuiguException(201, "排行榜信息不存在");
        }

        List<AlbumInfoIndex> albumInfoIndices = JSONObject.parseArray(albumInfoIndexList, AlbumInfoIndex.class);

        List<AlbumInfoIndexVo> albumInfoIndexVoList = albumInfoIndices.stream().map(albumInfoIndex -> {
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
            return albumInfoIndexVo;
        }).collect(Collectors.toList());

        return albumInfoIndexVoList;
    }

    /**
     * 异步多线程优化4次远程调用
     * <p>
     * A B C D
     * A线程--->B C D(并发执行)
     * <p>
     * C D A(并发执行)-->B线程
     *
     * @param albumId
     *
     * 缓存最快：只是会快一些。异步是压榨cpu.
     *
     * 异步多线程之后：快了100   第一次：226ms 第二次 25ms 第三次：29 第四次：26：除了第一次之外 后面的查询耗时平均在30ms.
     */
//    @SneakyThrows
//    @Override
//    public void albumOnSale(Long albumId) {
//
//
//        // 1.创建文档对象
//        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
//
//        Long startTime = System.currentTimeMillis();
//
//        // 2.给albumInfoIndex属性赋值
//        ConcurrentHashMap<String, Long> cMap = new ConcurrentHashMap<>();
//
//        Thread threadC = new Thread(new Runnable() {
//            // 2.3 远程查询专辑的分类信息
//            @Override
//            public void run() {
//                Result<BaseCategoryView> baseCategoryViewResult = albumInfoFeignClient.getAlbumCategory(albumId);
//                BaseCategoryView baseCategoryViewData = baseCategoryViewResult.getData();
//                Assert.notNull(baseCategoryViewData, "远程调用专辑微服务获取分类信息失败");
//
//                albumInfoIndex.setCategory1Id(baseCategoryViewData.getCategory1Id()); // 专辑一级分类id
//                albumInfoIndex.setCategory2Id(baseCategoryViewData.getCategory2Id()); // 专辑二级分类id
//                albumInfoIndex.setCategory3Id(baseCategoryViewData.getCategory3Id()); // 专辑二级分类id
//            }
//        }, "thread-C");
//        threadC.start();
//
//
//        Thread threadD = new Thread(new Runnable() {
//            // 2.4 远程查询专辑的统计信息
//            @Override
//            public void run() {
//                Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStat(albumId);
//                AlbumStatVo albumStatVoData = albumStatVoResult.getData();
//                if (albumStatVoData == null) {
//                    throw new GuiguException(201, "远程调用专辑微服务获取专辑分类信息失败");
//                }
//
//                Integer commentStatNum = albumStatVoData.getCommentStatNum();
//                Integer subscribeStatNum = albumStatVoData.getSubscribeStatNum();
//                Integer playStatNum = albumStatVoData.getPlayStatNum();
//                Integer buyStatNum = albumStatVoData.getBuyStatNum();
//                albumInfoIndex.setPlayStatNum(playStatNum);  // 专辑的播放量
//                albumInfoIndex.setSubscribeStatNum(subscribeStatNum); // 专辑的订阅量
//                albumInfoIndex.setBuyStatNum(buyStatNum); // 专辑的购买量
//                albumInfoIndex.setCommentStatNum(commentStatNum); // 专辑的评论数
////        Double hotScore = commentStatNum * 0.1 + subscribeStatNum * 0.2 + playStatNum * 0.4 + buyStatNum * 0.3; // 线上
//                Double hotScore = new Random().nextDouble(); // 测试环境用
//                albumInfoIndex.setHotScore(hotScore); // 专辑热度值
//
//            }
//        }, "thread-D");
//        threadD.start();
//
//
//
//        Thread threadA = new Thread(new Runnable() {
//            // 2.1 远程查询专辑基本信息
//            @Override
//            public void run() {
//                Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoAndAttrValue(albumId);
//                AlbumInfo albumInfoData = albumInfoResult.getData();
//                if (albumInfoData == null) {
//                    throw new GuiguException(201, "远程调用专辑微服务获取专辑信息失败");
//                }
//
//                albumInfoIndex.setId(albumInfoData.getId());  // 专辑id
//                albumInfoIndex.setAlbumTitle(albumInfoData.getAlbumTitle());   // 专辑标题
//                albumInfoIndex.setAlbumIntro(albumInfoData.getAlbumIntro()); // 专辑简介
//                albumInfoIndex.setCoverUrl(albumInfoData.getCoverUrl());  // 专辑封面
//                albumInfoIndex.setIncludeTrackCount(albumInfoData.getIncludeTrackCount()); // 专辑包含的声音集数
//                albumInfoIndex.setIsFinished(albumInfoData.getIsFinished().toString()); // 专辑是否完结
//                albumInfoIndex.setPayType(albumInfoData.getPayType()); // 专辑付费类型（免费 vip免费  付费）
//                albumInfoIndex.setCreateTime(new Date());  // 专辑保存到es的时间
//
//                List<AttributeValueIndex> attributeValueIndexs = albumInfoData.getAlbumAttributeValueVoList().stream().map(albumAttributeValue -> {
//                    AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
//                    attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
//                    attributeValueIndex.setValueId(albumAttributeValue.getValueId());
//                    return attributeValueIndex;
//                }).collect(Collectors.toList());
//                albumInfoIndex.setAttributeValueIndexList(Lists.newArrayList(attributeValueIndexs));   // 专辑的标签
//
//                cMap.put("userId", albumInfoData.getUserId());
//
//
//            }
//        }, "thread-A");
//        threadA.start();
//        threadA.join();   // tomcat线程先不往下走 等threadA线程干完活 tomcat线程才往下走,因为后面的远程 threadB线程
//        // 查询用户信息,需要用到 threadA 线程处理完的数据
//
//
//        Thread threadB = new Thread(new Runnable() {
//            // 2.2 远程查询用户信息
//            @Override
//            public void run() {
//                Long userId = cMap.get("userId");
//                Result<UserInfoVo> albumInfoVoResult = userInfoFeignClient.getUserInfo(userId);
//                UserInfoVo userInfoVoData = albumInfoVoResult.getData();
//                Assert.notNull(userInfoVoData, "远程调用用户微服务获取用户信息失败");
//                albumInfoIndex.setAnnouncerName(userInfoVoData.getNickname()); // 专辑对应的主播名字
//            }
//        }, "thread-B");
//        threadB.start();
//
//
//        // 3.将文档对象存储到 es中
//        Long endTime = System.currentTimeMillis();
//        log.info("专辑:{}上架到es耗时：{}ms", albumId, endTime - startTime);
//        albumInfoIndexRepository.save(albumInfoIndex);
//    }


    /**
     * 同步的方式：
     * 1.数据从哪里来 接着到哪里去
     * 2.读写模型
     * 3.请求响应模型
     * <p>
     * <p>
     * <p>
     * 如果单线程远程查询4次耗时：第一次：322ms 第二次：87ms 第三次62ms  第四次 56ms   平均在60ms
     * OpenFeign底层有缓存机制：只有在调用方去第一次调用时才会找注册中心要数据（发送HTTP请求） 接着将被调用方的信息缓存到了本地Map中，因此当调用方后续在
     * 给被调用发送请求时，直接从Map中获取被调用方的信息。
     *
     * 异步和快没有直接的关系。
     * 异步主要让线程压榨cpu.不让cpu空闲一直干活
     * @param albumId
     */
//    @Override
//    public void albumOnSale(Long albumId) {
//        // 1.创建文档对象
//        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
//
//        Long startTime = System.currentTimeMillis();
//
//        // 2.给albumInfoIndex属性赋值
//        // 查询tingshu_album库下的album_info表（只能用rpc远程调用：openFeign）
//        Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoAndAttrValue(albumId);
//        AlbumInfo albumInfoData = albumInfoResult.getData();
//        if (albumInfoData == null) {
//            throw new GuiguException(201, "远程调用专辑微服务获取专辑信息失败");
//        }
//
//        albumInfoIndex.setId(albumInfoData.getId());  // 专辑id
//        albumInfoIndex.setAlbumTitle(albumInfoData.getAlbumTitle());   // 专辑标题
//        albumInfoIndex.setAlbumIntro(albumInfoData.getAlbumIntro()); // 专辑简介
//        albumInfoIndex.setCoverUrl(albumInfoData.getCoverUrl());  // 专辑封面
//        albumInfoIndex.setIncludeTrackCount(albumInfoData.getIncludeTrackCount()); // 专辑包含的声音集数
//        albumInfoIndex.setIsFinished(albumInfoData.getIsFinished().toString()); // 专辑是否完结
//        albumInfoIndex.setPayType(albumInfoData.getPayType()); // 专辑付费类型（免费 vip免费  付费）
//        albumInfoIndex.setCreateTime(new Date());  // 专辑保存到es的时间
//
//        List<AttributeValueIndex> attributeValueIndexs = albumInfoData.getAlbumAttributeValueVoList().stream().map(albumAttributeValue -> {
//            AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
//            attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
//            attributeValueIndex.setValueId(albumAttributeValue.getValueId());
//            return attributeValueIndex;
//        }).collect(Collectors.toList());
//        albumInfoIndex.setAttributeValueIndexList(Lists.newArrayList(attributeValueIndexs));   // 专辑的标签
//
//        Result<UserInfoVo> albumInfoVoResult = userInfoFeignClient.getUserInfo(albumInfoData.getUserId());
//        UserInfoVo userInfoVoData = albumInfoVoResult.getData();
//        Assert.notNull(userInfoVoData, "远程调用用户微服务获取用户信息失败");
//        albumInfoIndex.setAnnouncerName(userInfoVoData.getNickname()); // 专辑对应的主播名字
//
//
//        Result<BaseCategoryView> baseCategoryViewResult = albumInfoFeignClient.getAlbumCategory(albumId);
//        BaseCategoryView baseCategoryViewData = baseCategoryViewResult.getData();
//        Assert.notNull(baseCategoryViewData, "远程调用专辑微服务获取分类信息失败");
//
//        albumInfoIndex.setCategory1Id(baseCategoryViewData.getCategory1Id()); // 专辑一级分类id
//        albumInfoIndex.setCategory2Id(baseCategoryViewData.getCategory2Id()); // 专辑二级分类id
//        albumInfoIndex.setCategory3Id(baseCategoryViewData.getCategory3Id()); // 专辑二级分类id
//
//
//        Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStat(albumId);
//
//        AlbumStatVo albumStatVoData = albumStatVoResult.getData();
//        if (albumStatVoData == null) {
//            throw new GuiguException(201, "远程调用专辑微服务获取专辑分类信息失败");
//        }
//
//        Integer commentStatNum = albumStatVoData.getCommentStatNum();
//        Integer subscribeStatNum = albumStatVoData.getSubscribeStatNum();
//        Integer playStatNum = albumStatVoData.getPlayStatNum();
//        Integer buyStatNum = albumStatVoData.getBuyStatNum();
//
//        albumInfoIndex.setPlayStatNum(playStatNum);  // 专辑的播放量
//        albumInfoIndex.setSubscribeStatNum(subscribeStatNum); // 专辑的订阅量
//        albumInfoIndex.setBuyStatNum(buyStatNum); // 专辑的购买量
//        albumInfoIndex.setCommentStatNum(commentStatNum); // 专辑的评论数
//
////        Double hotScore = commentStatNum * 0.1 + subscribeStatNum * 0.2 + playStatNum * 0.4 + buyStatNum * 0.3; // 线上
//
//        Double hotScore = new Random().nextDouble(); // 测试环境用
//        albumInfoIndex.setHotScore(hotScore); // 专辑热度值
//
//
//        // 3.将文档对象存储到 es 中
//        Long endTime = System.currentTimeMillis();
//
//        log.info("专辑:{}上架到es耗时：{}ms", albumId, endTime - startTime);
//        albumInfoIndexRepository.save(albumInfoIndex);
//
//
//    }
}
