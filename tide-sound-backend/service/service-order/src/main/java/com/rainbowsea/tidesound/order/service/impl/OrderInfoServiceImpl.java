package com.rainbowsea.tidesound.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.rainbowsea.tidesound.album.client.AlbumInfoFeignClient;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.rainbowsea.tidesound.model.user.VipServiceConfig;
import com.rainbowsea.tidesound.order.helper.SignHelper;
import com.rainbowsea.tidesound.order.mapper.OrderInfoMapper;
import com.rainbowsea.tidesound.order.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.rainbowsea.tidesound.vo.order.OrderDerateVo;
import com.rainbowsea.tidesound.vo.order.OrderDetailVo;
import com.rainbowsea.tidesound.vo.order.OrderInfoVo;
import com.rainbowsea.tidesound.vo.order.TradeVo;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;


    @Autowired
    private StringRedisTemplate redisTemplate;


    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {
        Long userId = AuthContextHolder.getUserId();

        // 1.创建OrderInfoVo对象
        OrderInfoVo orderInfoVo = null;

        String itemType = tradeVo.getItemType();
        switch (itemType) {
            case "1001":  // 专辑
                // 处理付款项类型是专辑的结算页
                orderInfoVo = dealItemTypeAlbum(tradeVo, userId);
                break;
            case "1002":  // 声音
                // 处理付款项类型是声音的结算页
                orderInfoVo = dealItemTypeTrack(tradeVo, userId);
                break;

            case "1003":  // vip套餐
                // 处理付款项类型是vip套餐的结算页
                orderInfoVo = dealItemTypeVip(tradeVo, userId);


        }

        // 签名设置
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        String sign = SignHelper.getSign(JSONObject.parseObject(JSONObject.toJSONString(orderInfoVo), Map.class));
        orderInfoVo.setSign(sign);


        String orderRepeatSubmitKey = userId + ":" + orderInfoVo.getTradeNo();
        redisTemplate.opsForValue().set(orderRepeatSubmitKey, "1");
        return orderInfoVo;

        // orderInfoVo--- originalAmount:100
        // Map（originalAmount:100）
        // 100 | "atguigu123"--->md5--->"123456789"
    }

    @Override
    public Map<String, Object> submitOrder(OrderInfoVo orderInfoVo) {

        // MD5-->不可逆
        // "abcd"---md5--->"123abcdef"
        // "abc"----md5--->"234abcdef"

        // orderInfoVo(originalAmount:200)--->Map(originalAmount:200)---签名---200 | "atguigu123"---md5---"123456789xb"


        // 本质：将提交给我们的数据存储到对应的数据库中表下。


        // think--->1.将前端提交的数据 保存到tingshu_order库下的三种表  2.生成订单编号 3.支付方法的判断【零钱支付 微信支付】  4.将订单编号返回

        // 1.做提交订单的校验
        // 1.1 基础数据的校验---spring做好了。
        // 1.2 业务层的校验
        // a)验证订单信息的完整性
        // b)验证订单信息的时效性--主要保证订单尽可能小的被篡改的风险
        // c)重复提交的校验
        // d)非法请求的校验
        // e)请求次数限制的校验
        // e)请求的ip做地域的校验
        // f)请求ip做黑白名单校验
        // g)在窗口期做请求速率的限制
        // .....

        // 1.业务层的校验
        // 1.1 校验请求的非法性。
        String tradeNo = orderInfoVo.getTradeNo();
        if (StringUtils.isEmpty(tradeNo)) {
            throw new GuiguException(201, "非法请求");
        }
        // 1.2 校验订单信息的完整性以及时效性
        String orderInfoVoStr = JSONObject.toJSONString(orderInfoVo);
        Map signMap = JSONObject.parseObject(orderInfoVoStr, Map.class);
        signMap.put("payWay", "");
        SignHelper.checkSign(signMap); // 先在做一次新签名---接着获取之前的签名---最后对比这两个签名是否相等，如果相等代表没有被篡改过 如果不相等 一定被篡改过


        // 1.3 订单重复提交的校验
        //  第一次提交订单之后，未看见后续的动作（1、网络原因 2.接口的并发高，处理慢） 但是你误以为失败或者以为没点上 接着又点击一次。【这就导致了重复提交】
        //  解决办法：对于同一个请求来说，不管调用多少次，数据都要是一致的,产生的结果是符合预期的。
        //  接口幂等性保证：方案：各种锁机制（分布式锁 本地锁） MySQL的唯一索引+防重表+本地事务  Redis+Token(最多)
        // 1(订单id)--->1001(订单编号)--->"买了个飞机"（订单名字）---100（订单的价格）
        // 1(订单id)--->1001(订单编号)--->"买了个飞机"（订单名字）---100（订单的价格）
        // 单端重复提交  ---TODO:多端重复提交如何实现。
        String orderRepeatSubmitKey = AuthContextHolder.getUserId() + ":" + orderInfoVo.getTradeNo();
        String luaScript = "if redis.call(\"exists\",KEYS[1])\n" + "then\n" + "    return redis.call(\"del\",KEYS[1])\n" + "else\n" + "    return 0\n" + "end";
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(luaScript, Long.class), Arrays.asList(orderRepeatSubmitKey));
        if(execute==0){
            throw new GuiguException(201,"订单重复提交");
        }

        // 2.保存订单信息(核心代码)


        // 3.生成订单编号

        // 4.判断支付方式

        // 5.订单编号返回


        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", "");



        return map;

    }


    /**
     * 处理付款项类型是vip套餐的结算页
     *
     * @param tradeVo
     * @param userId
     * @return
     */
    private OrderInfoVo dealItemTypeVip(TradeVo tradeVo, Long userId) {

        // 1.构建结算页对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();

        // 2.1.校验 某一个套餐是否支付过（不用做）

        // 2.2 校验 某一个套餐是否下单未支付（要做） 如果用户下单了某一个套餐 但是还未支付 接着该用户在次点击购买该套餐，提示当前用户该套餐已下单，准备支付 请勿在次提交。

        Long count = orderInfoMapper.getItemTypeAlbumAndVipIsPadding(userId, tradeVo.getItemType(), tradeVo.getItemId());
        if (count > 0) {
            log.info("当前购买的某一个套餐下单未支付，请勿重复下单!");
            List<Long> exitItemIds = new ArrayList<>();
            exitItemIds.add(tradeVo.getItemId());
            orderInfoVo.setExitItemIdList(exitItemIds);
            return orderInfoVo;
        }
        String tradeNo = RandomStringUtils.random(10, false, true);
        orderInfoVo.setTradeNo(tradeNo);
        orderInfoVo.setPayWay("");
        orderInfoVo.setItemType(tradeVo.getItemType());

        Result<VipServiceConfig> vipServiceConfigResult = userInfoFeignClient.getVipConfigById(tradeVo.getItemId());
        VipServiceConfig serviceConfigResultData = vipServiceConfigResult.getData();


        if(serviceConfigResultData == null) {
            throw new GuiguException(201, "远程查询用户微服务获取套餐信息失败");
        }

        BigDecimal originalAmount = serviceConfigResultData.getPrice(); // 获取套餐原始金额
        BigDecimal discountPrice = serviceConfigResultData.getDiscountPrice();// 获取套餐优惠金额
        orderInfoVo.setOriginalAmount(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP)); // 原始金额
        orderInfoVo.setDerateAmount(originalAmount.subtract(discountPrice).setScale(2, BigDecimal.ROUND_HALF_UP)); // 优惠掉多少金额
        orderInfoVo.setOrderAmount(discountPrice.setScale(2, BigDecimal.ROUND_HALF_UP)); // 实际金额


        // 构建结算页详情
        List<OrderDetailVo> orderDetailVos = new ArrayList<>();
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(tradeVo.getItemId());
        orderDetailVo.setItemName(serviceConfigResultData.getName());
        orderDetailVo.setItemUrl(serviceConfigResultData.getImageUrl());
        orderDetailVo.setItemPrice(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));// 原始金额
        orderDetailVos.add(orderDetailVo);
        orderInfoVo.setOrderDetailVoList(com.google.common.collect.Lists.newArrayList(orderDetailVos));

        // 构建结算页减免
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        OrderDerateVo orderDerateVo = new OrderDerateVo();
        orderDerateVo.setDerateType("1406-VIP服务折扣");
        orderDerateVo.setDerateAmount(discountPrice.setScale(2, BigDecimal.ROUND_HALF_UP));  // 优惠后
        orderDerateVo.setRemarks("套餐参与了优惠");
        orderDerateVoList.add(orderDerateVo);
        orderInfoVo.setOrderDerateVoList(com.google.common.collect.Lists.newArrayList(orderDerateVoList));

        orderInfoVo.setTimestamp(System.currentTimeMillis());

        return orderInfoVo;
    }

    /**
     * 处理付款项类型是声音的结算页
     *
     * @param tradeVo
     * @param userId
     * @return
     */
    private OrderInfoVo dealItemTypeTrack(TradeVo tradeVo, Long userId) {

        OrderInfoVo orderInfoVo = new OrderInfoVo();

        // 0.该声音是否支付过（一定是没有支付过）在分集展示声音的时候 已经将买过的声音过滤掉了（幂等性不用在做了）

        // 1.查询要买那些声音
        Long trackId = tradeVo.getItemId();  // 当前声音id
        Integer trackCount = tradeVo.getTrackCount();

        // 1.1 查询声音对象
        Result<TrackInfo> trackInfoResult = albumInfoFeignClient.getTrackInfoByTrackId(trackId);
        TrackInfo trackInfoResultData = trackInfoResult.getData();
        if (trackInfoResultData == null) {
            throw new GuiguException(201, "该声音不存在");
        }

        // 1.2 查询专辑对象
        Long albumId = trackInfoResultData.getAlbumId();
        Result<AlbumInfo> albumInfoAndAttrValue = albumInfoFeignClient.getAlbumInfoAndAttrValue(albumId);
        AlbumInfo albumInfo = albumInfoAndAttrValue.getData();
        if (albumInfo == null) {
            throw new GuiguException(201, "该声音对应的专辑不存在");
        }
        // 1.3 查询指定集数的声音列表
        Result<List<TrackInfo>> trackListByCurrentTrackId = albumInfoFeignClient.getTrackListByCurrentTrackId(userId, trackId, trackCount);
        List<TrackInfo> trackIdData = trackListByCurrentTrackId.getData();
        if (CollectionUtils.isEmpty(trackIdData)) {
            throw new GuiguException(201, "远程查询专辑微服务获取指定集数的声音失败");
        }


        // 2.该声音是否下单但是未支付（幂等性要做）
        List<Long> submitAndUnPaidTrackIds = orderInfoMapper.getItemTypeTrackIsPadding(userId, tradeVo.getItemType());
        List<TrackInfo> trackInfoList = trackIdData.stream().filter(trackInfo -> submitAndUnPaidTrackIds.contains(trackInfo.getId())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(trackInfoList)) {

            List<Long> exitTrackIds = new ArrayList<>();
            for (TrackInfo trackInfo : trackInfoList) {
                exitTrackIds.add(trackInfo.getId());
            }
            orderInfoVo.setExitItemIdList(exitTrackIds);
            log.error("该声音已下单，请勿重复购买！");
            return orderInfoVo;
        } else {

            String tradeNo = RandomStringUtils.random(10, false, true);
            orderInfoVo.setTradeNo(tradeNo);
            orderInfoVo.setPayWay("");
            orderInfoVo.setItemType(tradeVo.getItemType());
            BigDecimal price = albumInfo.getPrice(); // 单集声音的价格
            BigDecimal originalAmount = trackCount == 0 ? price : price.multiply(new BigDecimal(trackCount));

            orderInfoVo.setOriginalAmount(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));  // 原始金额
            orderInfoVo.setDerateAmount(new BigDecimal("0.00")); // 优惠金额  (声音没有优惠)
            orderInfoVo.setOrderAmount(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP)); // 实际金额

            List<OrderDetailVo> orderDetailVos = new ArrayList<>();
            // 封装结算页详情数据
            for (TrackInfo trackInfo : trackIdData) {
                OrderDetailVo orderDetailVo = new OrderDetailVo();

                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemPrice(price.setScale(2, BigDecimal.ROUND_HALF_UP));// 每一集声音价格

                orderDetailVos.add(orderDetailVo);
            }
            orderInfoVo.setOrderDetailVoList(com.google.common.collect.Lists.newArrayList(orderDetailVos));

            // 封装结算减免数据
            orderInfoVo.setOrderDerateVoList(com.google.common.collect.Lists.newArrayList());
        }
        // 3.返回结算页对象
        return orderInfoVo;
    }


    /**
     * 处理付款项类型是专辑的结算页
     *
     * @param tradeVo
     * @param userId
     * @return
     */
    private OrderInfoVo dealItemTypeAlbum(TradeVo tradeVo, Long userId) {

        // 1.创建结算页对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        Long itemId = tradeVo.getItemId();

        // 1.1 查询要购买的该专辑是否已经支付过（幂等性校验）
        Result<Boolean> userPaidAlbum = userInfoFeignClient.getUserPaidAlbum(userId, itemId);
        Boolean aBoolean = userPaidAlbum.getData();
        if (aBoolean == null) {
            throw new GuiguException(201, "远程调用用户微服务获取用户是否购买过专辑失败");
        }
        if (aBoolean) {
            log.info("该专辑已经购买过，请勿重复购买!");
            List<Long> exitItemIds = new ArrayList<>();
            exitItemIds.add(itemId);
            orderInfoVo.setExitItemIdList(exitItemIds);
            return orderInfoVo;
        }

        // 1.2 查询要购买的该专辑是下单但是未支付（幂等性校验）
        Long count = orderInfoMapper.getItemTypeAlbumAndVipIsPadding(userId, tradeVo.getItemType(), itemId);
        if (count > 0) {
            log.info("当前购买的专辑下单未支付，请勿重复下单!");
            List<Long> exitItemIds = new ArrayList<>();
            exitItemIds.add(itemId);
            orderInfoVo.setExitItemIdList(exitItemIds);
            return orderInfoVo;
        }


        // 2.给结算页对象赋值
        String tradeNo = RandomStringUtils.random(10, false, true);
        orderInfoVo.setTradeNo(tradeNo); // 防止订单重复提交--->跟踪订单
        orderInfoVo.setPayWay("");// 支付方式不知道
        orderInfoVo.setItemType(tradeVo.getItemType()); // 付款项类型

        // 2.1 远程查询专辑对象
        Result<AlbumInfo> albumInfoAndAttrValue = albumInfoFeignClient.getAlbumInfoAndAttrValue(itemId);
        AlbumInfo albumInfoData = albumInfoAndAttrValue.getData();
        if (albumInfoData == null) {
            throw new GuiguException(201, "远程查询专辑信息失败");
        }

        // 2.2 远程查询用户信息
        Result<UserInfoVo> userInfo = userInfoFeignClient.getUserInfo(userId);
        UserInfoVo userInfoData = userInfo.getData();

        if (userInfoData == null) {
            throw new GuiguException(201, "远程查询用户微服务获取用户信息失败");
        }

        Integer isVip = userInfoData.getIsVip();

        // 2.3 获取折扣粒度
        BigDecimal disCount = new BigDecimal("0.00");// 折扣粒度
        // vip
        if ((isVip + "").equals("1")) {
            disCount = albumInfoData.getVipDiscount();
        } else {
            disCount = albumInfoData.getDiscount();
        }
        if (disCount.intValue() == -1) {// 不打折
            disCount = new BigDecimal("10.00");
        }

        //. 2.4 原始金额
        BigDecimal originalPrice = albumInfoData.getPrice();// 专辑的价格 未优惠的
        orderInfoVo.setOriginalAmount(originalPrice.setScale(2, BigDecimal.ROUND_HALF_UP));  // 专辑的原始金额（未参与打折优惠的）

        // 100 7  30=（100-7*10）;   // 原价 30  打折：7 （10）  实际：21 30  优惠30-21=9
        // 2.5 实际金额
        BigDecimal orderAmount = originalPrice.multiply(disCount).divide(new BigDecimal(10)).setScale(2, BigDecimal.ROUND_HALF_UP);

        // 2.6 优惠金额
        BigDecimal derateAmount = originalPrice.subtract(orderAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        orderInfoVo.setDerateAmount(derateAmount);// 专辑的减免金额：【原件--折扣价】 原价：100  折扣后的金额 70  优惠的价格：30（100-70=30）
        orderInfoVo.setOrderAmount(orderAmount);// 实际金额

        // 2.7 构建订单详情明细
        List<OrderDetailVo> orderDetailVos = new ArrayList<>();
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(itemId); // 专辑id
        orderDetailVo.setItemName(albumInfoData.getAlbumTitle()); // 专辑的名字
        orderDetailVo.setItemUrl(albumInfoData.getCoverUrl()); // 专辑的封面
        orderDetailVo.setItemPrice(originalPrice); // 专辑的原始金额
        orderDetailVos.add(orderDetailVo);
        orderInfoVo.setOrderDetailVoList(Lists.newArrayList(orderDetailVos));  // 订单明细：到底买的是什么东西

        // 2.8 构建订单减免明细
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        OrderDerateVo orderDerateVo = new OrderDerateVo();
        orderDerateVo.setDerateType("1405-专辑折扣");
        orderDerateVo.setDerateAmount(derateAmount);
        orderDerateVo.setRemarks("专辑打折了");
        orderDerateVoList.add(orderDerateVo);
        orderInfoVo.setOrderDerateVoList(Lists.newArrayList(orderDerateVoList)); // 减免明细：到底减免的东西是谁
        orderInfoVo.setTimestamp(System.currentTimeMillis()); // 订单结算页数据的时间戳（为了做订单完整性校验）


        // 3.将结算页对象返回
        return orderInfoVo;
    }
}
