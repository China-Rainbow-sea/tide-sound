package com.rainbowsea.tidesound.search.service.impl;

import com.google.common.collect.Lists;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.BaseCategoryView;
import com.rainbowsea.tidesound.model.search.AlbumInfoIndex;
import com.rainbowsea.tidesound.model.search.AttributeValueIndex;
import com.rainbowsea.tidesound.search.repository.AlbumInfoIndexRepository;
import com.rainbowsea.tidesound.search.service.ItemService;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.rainbowsea.tidesound.vo.album.AlbumStatVo;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ItemServiceImpl implements ItemService {


    // 定义操作 ES 实现类
    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;


    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;


    // 这个线程池，默认 4 个线程(因为我们这里线程配合只需要 4 个线程 减减即可)
    ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * 异步：线程池：（countdownlatch使用）第一次259ms   后面的平均在30ms作用
     *
     * @param albumId
     */
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
                    org.springframework.util.Assert.notNull(userInfoVoData, "远程调用用户微服务获取用户信息失败");
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

    }

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
