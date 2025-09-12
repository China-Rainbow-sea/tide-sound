package com.rainbowsea.tidesound.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.album.mapper.AlbumAttributeValueMapper;
import com.rainbowsea.tidesound.album.mapper.AlbumInfoMapper;
import com.rainbowsea.tidesound.album.mapper.AlbumStatMapper;
import com.rainbowsea.tidesound.album.mapper.TrackInfoMapper;
import com.rainbowsea.tidesound.album.service.AlbumAttributeValueService;
import com.rainbowsea.tidesound.album.service.AlbumInfoService;
import com.rainbowsea.tidesound.common.constant.SystemConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.rabbit.constant.MqConst;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.album.AlbumAttributeValue;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.model.album.AlbumStat;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.rainbowsea.tidesound.query.album.AlbumInfoQuery;
import com.rainbowsea.tidesound.vo.album.AlbumAttributeValueVo;
import com.rainbowsea.tidesound.vo.album.AlbumInfoVo;
import com.rainbowsea.tidesound.vo.album.AlbumListVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;


    @Autowired
    private AlbumStatMapper albumStatMapper;


    @Autowired
    private TrackInfoMapper trackInfoMapper;


    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    // spring的事务来说 只能对两种异常进行回滚（运行时异常以及Error）
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo) {

        // 为当前类，生成一个代理对象，使用该方法，需要在启动类上加上 @EnableAspectJAutoProxy(exposeProxy = true)  // 开启 Spring 的AOP 自动代理功能，并确保内部方法调用时也能触发代理逻辑
        AlbumInfoServiceImpl proxyObject = (AlbumInfoServiceImpl) AopContext.currentProxy();

        // 入库 tingshu_album 表（album_info  album_attribute_value）
        // 因为添加了 @Tingshu 登录认证(其中将userId存储到了LocalThread线程当中了)，同一个线程可以获取到存储的信息
        Long userId = AuthContextHolder.getUserId();


        // 1.保存专辑基本信息（album_info表中插入数据）
        // 实体类专门用来和数据库列做映射
        AlbumInfo albumInfo = new AlbumInfo();
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        albumInfo.setUserId(userId);  // 额外赋值一次（userId）
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS); // 默认审核通过（TODO 对接后续系统）
//        albumInfo.setIncludeTrackCount(50);
        //获取专辑的付费类型(0101（免费） 0102(vip免费)  0103(付费))
        String payType = albumInfoVo.getPayType();
        if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(payType)) {
            // 默认给这个专辑设置5集免费声音
            albumInfo.setTracksForFree(5);
        }

        int insert = albumInfoMapper.insert(albumInfo);
        log.info("保存专辑基本信息：{}", insert > 0 ? "success" : "fail");


        // 2.保存专辑的标签信息（album_attribute_value表中插入数据）
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList(); // 获取选择的标签信息

        List<AlbumAttributeValue> attributeValues = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
            AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
            albumAttributeValue.setAlbumId(albumInfo.getId());
            albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());  // 专辑的属性id
            albumAttributeValue.setValueId(albumAttributeValueVo.getValueId()); // 属性值id
            return albumAttributeValue;
        }).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(attributeValues)) {
            boolean b = albumAttributeValueService.saveBatch(attributeValues);
            log.info("保存专辑标签信息：{}", b ? "sucess" : "fail");
        }


        // 3.保存专辑的统计（album_stat）
        // saveAlbumStat(albumInfo.getId());
        // 代理对象.saveAlbumStat(albumInfo.getId()); // 事务不会失效
        // tempService.saveAlbumStat(albumInfo.getId());  // 代理对象调
        // albumInfoService.saveAlbumStat(albumInfo.getId()); // 代理对象调
        proxyObject.saveAlbumStat(albumInfo.getId()); // 代理对象 (建议这种写法)

    }

    @Override
    public IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> pageParam, AlbumInfoQuery albumInfoQuery) {


        return albumInfoMapper.findUserAlbumPage(pageParam, albumInfoQuery);
    }

    @Override
    public AlbumInfo getAlbumInfo(Long albumId) {


        // 1.根据专辑id查询专辑基本信息
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new GuiguException(201, "该专辑不存在");
        }


        // 2.查询专辑的标签信息（属性id和属性值id）
        List<AlbumAttributeValue> attributeValues = albumAttributeValueMapper.selectList(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));

        albumInfo.setAlbumAttributeValueVoList(attributeValues);

        return albumInfo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo) {
        // 1.修改专辑的基本信息
        // 1.1. 根据专辑id查询专辑的旧数据
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new GuiguException(201, "该专辑不存在");
        }


        BeanUtils.copyProperties(albumInfoVo, albumInfo); // 有的属性直接拷贝过期  vo没有的属性用老的AlbumInfo

        // 修改为免费的，需要额外修改如下属性
        if (SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfoVo.getPayType())) {

            albumInfo.setPrice(new BigDecimal("0.00"));
            albumInfo.setVipDiscount(new BigDecimal("-1.00"));
            albumInfo.setDiscount(new BigDecimal("-1.00"));
            albumInfo.setTracksForFree(0);
        } else {
            // 其他vip/ 付费的，试看 5 集
            albumInfo.setTracksForFree(5);
        }
        int i = albumInfoMapper.updateById(albumInfo);
        log.info("修改专辑的基本信息{}", i > 0 ? "success" : "fail");


        // 2.专辑的标签信息
        // 2.1 删除该专辑的老标签信息
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        int delete = albumAttributeValueMapper.delete(wrapper);
        log.info("删除专辑的旧标签数据：{}", delete > 0 ? "success" : "fail");


        // 2.2 查询该专辑的新标签信息
        List<AlbumAttributeValue> attributeValues = albumInfoVo.getAlbumAttributeValueVoList().stream().map(albumAttributeValueVo -> {
            AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
            albumAttributeValue.setAlbumId(albumId);
            albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
            albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());

            return albumAttributeValue;

        }).collect(Collectors.toList());


        if (!CollectionUtils.isEmpty(attributeValues)) {
            boolean b = albumAttributeValueService.saveBatch(attributeValues);
            log.info("保存专辑标签信息：{}", b ? "success" : "fail");
        }


    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeAlbumInfo(Long albumId) {

        // 1.删除专辑的基本信息
        // 查询专辑下是否有声音
        Long count = trackInfoMapper.selectCount(new LambdaQueryWrapper<TrackInfo>().eq(TrackInfo::getAlbumId, albumId));
        if (count > 0) {
            throw new GuiguException(201, "该专辑下存在声音，请勿删除!");
        }
        int i = albumInfoMapper.deleteById(albumId);
        log.info("删除专辑的基本信息：{}", i > 0 ? "success" : "fail");

        // 2.删除专辑的标签信息
        int delete = albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        log.info("删除专辑的标签信息：{}", delete > 0 ? "success" : "fail");

        // 3.专辑的统计信息
        int delete1 = albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        log.info("删除专辑的统计信息：{}", delete1 > 0 ? "success" : "fail");



    }

    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        return albumInfoMapper.selectList(new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getUserId, userId));
    }

    /**
     *  保存专辑的统计（album_stat） 操作  album_stat 数据表
     * @param albumId
     */
    @Transactional
    public void saveAlbumStat(Long albumId) {

        ArrayList<String> albumStatus = new ArrayList<>();
        albumStatus.add(SystemConstant.ALBUM_STAT_PLAY);
        albumStatus.add(SystemConstant.ALBUM_STAT_SUBSCRIBE);
        albumStatus.add(SystemConstant.ALBUM_STAT_BROWSE);
        albumStatus.add(SystemConstant.ALBUM_STAT_COMMENT);
        for (String status : albumStatus) {
            AlbumStat albumStat = new AlbumStat();
            albumStat.setAlbumId(albumId);
            albumStat.setStatType(status);
            albumStat.setStatNum(0);
            albumStatMapper.insert(albumStat);
        }
    }
}
