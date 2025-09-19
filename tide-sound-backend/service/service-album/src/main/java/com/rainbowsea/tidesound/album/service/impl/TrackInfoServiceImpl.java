package com.rainbowsea.tidesound.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rainbowsea.tidesound.album.mapper.AlbumInfoMapper;
import com.rainbowsea.tidesound.album.mapper.TrackInfoMapper;
import com.rainbowsea.tidesound.album.mapper.TrackStatMapper;
import com.rainbowsea.tidesound.album.service.TrackInfoService;
import com.rainbowsea.tidesound.album.service.VodService;
import com.rainbowsea.tidesound.common.constant.SystemConstant;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import com.rainbowsea.tidesound.model.album.TrackInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rainbowsea.tidesound.model.album.TrackStat;
import com.rainbowsea.tidesound.query.album.TrackInfoQuery;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.rainbowsea.tidesound.vo.album.AlbumTrackListVo;
import com.rainbowsea.tidesound.vo.album.TrackInfoVo;
import com.rainbowsea.tidesound.vo.album.TrackListVo;
import com.rainbowsea.tidesound.vo.album.TrackMediaInfoVo;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    // 腾讯云-云点播服务
    @Autowired
    private VodService vodService;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private TrackStatMapper trackStatMapper;


    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {


        return vodService.uploadTrack(file);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveTrackInfo(TrackInfoVo trackInfoVo) {

        // 获取当前类的代理对象
        TrackInfoServiceImpl proxyObject = (TrackInfoServiceImpl) AopContext.currentProxy();

        // 1.保存声音基本信息
        TrackInfo trackInfo = new TrackInfo();
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        // 因为添加了 @Tingshu 登录认证(其中将userId存储到了LocalThread线程当中了)，同一个线程可以获取到存储的信息
        trackInfo.setUserId(AuthContextHolder.getUserId());

        trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);  // 声音默认审核通过


        // 1.1 处理声音的orderNum（这个声音在当前专辑中的序列号）
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, trackInfoVo.getAlbumId());
        wrapper.orderByDesc(TrackInfo::getOrderNum);
        wrapper.last("limit 1");
        TrackInfo trackInfo1 = trackInfoMapper.selectOne(wrapper);
        Integer orderNum = trackInfo1 == null ? 1 : trackInfo1.getOrderNum() + 1;
        trackInfo.setOrderNum(orderNum);

        // 1.2 处理声音的媒体信息
        TrackMediaInfoVo trackMediaInfoVo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
        if (trackMediaInfoVo == null) {
            throw new GuiguException(201, "该声音对应的媒体信息不存在!");
        }
        trackInfo.setMediaSize(trackMediaInfoVo.getSize());
        trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
        trackInfo.setMediaType(trackMediaInfoVo.getType());
        trackInfo.setMediaDuration(new BigDecimal(trackMediaInfoVo.getDuration()));
        int insert = trackInfoMapper.insert(trackInfo);
        log.info("保存声音基本信息：{}", insert > 0 ? "success" : "fail");

        // 2.保存声音的统计信息
        Integer integer = proxyObject.saveTrackStat(trackInfo.getId());
        log.info("保存声音统计信息：{}", integer > 0 ? "success" : "fail");


        // 3.反向更新专辑的包含声音集数属性
        Long albumId = trackInfo.getAlbumId();
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new GuiguException(201, "该声音对应的专辑不存在");
        }
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        int i = albumInfoMapper.updateById(albumInfo);
        log.info("更新对应专辑：{}", i > 0 ? "success" : "fail");

    }

    @Override
    public IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageParam, TrackInfoQuery trackInfoQuery) {
        return trackInfoMapper.findUserTrackPage(pageParam, trackInfoQuery);
    }

    @Override
    public void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo) {


        // 修改声音基本信息表
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        if (trackInfo == null) {
            throw new GuiguException(201, "该声音不存在");
        }

        String mediaFileIdNew = trackInfoVo.getMediaFileId();
        if (StringUtils.isEmpty(mediaFileIdNew)) {
            throw new GuiguException(201, "修改的声音源信息不存在");
        }
        String mediaFileIdOld = trackInfo.getMediaFileId();

        BeanUtils.copyProperties(trackInfoVo, trackInfo);

        if (!StringUtils.isEmpty(mediaFileIdNew) && !mediaFileIdNew.equals(mediaFileIdOld)) {
            // 修改声音的时候 重新上传了一个新声音
            TrackMediaInfoVo trackMediaInfoVo = vodService.getMediaInfo(mediaFileIdNew);
            if (trackMediaInfoVo == null) {
                throw new GuiguException(201, "上传的新声音不存在");
            }
            trackInfo.setMediaSize(trackMediaInfoVo.getSize());
            trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
            trackInfo.setMediaType(trackMediaInfoVo.getType());
            trackInfo.setMediaDuration(new BigDecimal(trackMediaInfoVo.getDuration()));
        }
        trackInfoMapper.updateById(trackInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long trackId) {

        // 1.修改该声音对应专辑的声音集数
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        if (trackInfo == null) {
            throw new GuiguException(201, "该声音已经不存在");
        }
        Long albumId = trackInfo.getAlbumId();

        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new GuiguException(201, "该声音对应的专辑不存在");
        }
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        albumInfoMapper.updateById(albumInfo);

        // 2.删除声音基本信息
        trackInfoMapper.deleteById(trackId);

        // 3.删除声音统计信息
        trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));

        // 4.删除vod中的声音（磁盘中临时目录的文件删掉）

        vodService.removeMediaFile(trackInfo.getMediaFileId());

    }


    /**
     * 保存声音的统计信息
     *
     * @param trackId
     * @return
     */
    @Transactional
    public Integer saveTrackStat(Long trackId) {

        ArrayList<String> trackStatus = new ArrayList<>();
        trackStatus.add(SystemConstant.TRACK_STAT_PLAY);
        trackStatus.add(SystemConstant.TRACK_STAT_COLLECT);
        trackStatus.add(SystemConstant.TRACK_STAT_PRAISE);
        trackStatus.add(SystemConstant.TRACK_STAT_COMMENT);
        try {
            for (String status : trackStatus) {
                TrackStat trackStat = new TrackStat();
                trackStat.setTrackId(trackId);
                trackStat.setStatType(status);
                trackStat.setStatNum(0);
                trackStatMapper.insert(trackStat);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // region ---------- start  根据专辑 id 查询专辑下声音列表且显示付费图标   ---------

    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> albumTrackListVoPage, Long albumId) {

        IPage<AlbumTrackListVo> result = new Page<AlbumTrackListVo>();


        // 1.查询该专辑
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new GuiguException(201, "该专辑不存在");
        }

        // 2.查询该专辑下的声音列表（分页）
        albumTrackListVoPage = trackInfoMapper.getAlbumTrackListByAlbumIdFromDb(albumTrackListVoPage, albumId);


        // 3.查询专辑的其它信息(免费 vip免费 付费)
        String payType = albumInfo.getPayType();
        String priceType = albumInfo.getPriceType(); // 单集 整专辑
        Integer tracksForFree = albumInfo.getTracksForFree();  // 包含声音的集数
        Long userId = AuthContextHolder.getUserId();

        switch (payType) {
            case "0101":
                result = albumTrackListVoPage;
                break;
            case "0102":
                // 处理vip免费类型
                result = this.dealAlbumPayTypeVip(albumId, userId, priceType, tracksForFree, albumTrackListVoPage);
                break;
            case "0103":
                // 处理付费
                result = this.dealAlbumPayTypeNeedPay(albumId, userId, priceType, tracksForFree, albumTrackListVoPage);
        }

        return result;

    }


    /**
     * 处理付费，是否显示付费图标
     *
     * @param albumId
     * @param userId
     * @param priceType
     * @param tracksForFree
     * @param albumTrackListVoPage
     * @return
     */
    private IPage<AlbumTrackListVo> dealAlbumPayTypeNeedPay(Long albumId, Long userId, String priceType, Integer tracksForFree, IPage<AlbumTrackListVo> albumTrackListVoPage) {

        // 1.判断用户id是否存在
        if (userId == null) {
            List<AlbumTrackListVo> albumTrackListVoList = albumTrackListVoPage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() <= 5).collect(Collectors.toList());
            return albumTrackListVoPage.setRecords(albumTrackListVoList);
        }
        // 2.用户登录了
        return getReallyShowAlbumTrackList(albumId, userId, priceType, tracksForFree, albumTrackListVoPage);

    }

    /**
     * 处理vip付费类型图标展示
     *
     * @param albumId
     * @param userId
     * @param priceType
     * @param tracksForFree
     * @param albumTrackListVoPage
     * @return
     */
    private IPage<AlbumTrackListVo> dealAlbumPayTypeVip(Long albumId, Long userId, String priceType, Integer tracksForFree, IPage<AlbumTrackListVo> albumTrackListVoPage) {

        // 1.判断用户id是否存在
        if (userId == null) {
            // 展示5集免费的声音返回给前端
            List<AlbumTrackListVo> albumTrackListVoList = albumTrackListVoPage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() <= 5).collect(Collectors.toList());
            return albumTrackListVoPage.setRecords(albumTrackListVoList);
        }

        // 2.用户id存在
        Result<UserInfoVo> userInfo = userInfoFeignClient.getUserInfo(userId);
        UserInfoVo userInfoData = userInfo.getData();
        if (userInfoData == null) {
            throw new GuiguException(201, "该用户信息不存在");
        }
        Integer isVip = userInfoData.getIsVip();
        Date vipExpireTime = userInfoData.getVipExpireTime();

        // 3.判断是否是vip
        //.如果是vip 直接返回所有的声音 付费图标不用修改 默认的
        if ("0".equals(isVip + "") || ("1".equals(isVip + "") && vipExpireTime.before(new Date()))) {
            return getReallyShowAlbumTrackList(albumId, userId, priceType, tracksForFree, albumTrackListVoPage);
        } else {
            // 是vip且还没过期
            return albumTrackListVoPage;
        }
    }

    /**
     * 处理购买过某集的显示专辑曲目列表（购买了该集，就显示付费图标）
     *
     * @param albumId
     * @param userId
     * @param priceType
     * @param tracksForFree
     * @param albumTrackListVoPage
     * @return
     */
    private IPage<AlbumTrackListVo> getReallyShowAlbumTrackList(Long albumId, Long userId, String priceType, Integer tracksForFree, IPage<AlbumTrackListVo> albumTrackListVoPage) {
        // think 如果某一集声音你购买过 那么这一集声音的付费图标就不用显示（使用默认的）  反之 如果这集声音 你没有买过且还不在免费集数之内 那么就要展示付费图标
        // 判断价格类型
        // 3.1 如果是单集购买
        if ("0201".equals(priceType)) {
            // a)查询当前用户购买过当前专辑下的哪些声音
            Result<Map<Long, String>> userPaidAlbumTrackMap = userInfoFeignClient.getUserPaidAlbumTrack(userId, albumId);
            Map<Long, String> userPaidAlbumTrackMapData = userPaidAlbumTrackMap.getData();
            if (userPaidAlbumTrackMapData == null) {
                throw new GuiguException(201, "远程查询用户微服务获取用户购买过专辑下的声音失败");
            }
            List<AlbumTrackListVo> reallyShowAlbumTrackList = albumTrackListVoPage.getRecords().stream().map(albumTrackListVo -> {
                if (StringUtils.isEmpty(userPaidAlbumTrackMapData.get(albumTrackListVo.getTrackId())) && albumTrackListVo.getOrderNum() > tracksForFree) {
                    albumTrackListVo.setIsShowPaidMark(true);
                }
                return albumTrackListVo;
            }).collect(Collectors.toList());
            return albumTrackListVoPage.setRecords(reallyShowAlbumTrackList);
        } else {
            // 3.2 整专辑购买
            Result<Boolean> userPaidAlbum = userInfoFeignClient.getUserPaidAlbum(userId, albumId);
            Boolean isPaidAlbum = userPaidAlbum.getData();
            if (isPaidAlbum == null) {
                throw new GuiguException(201, "远程查询用户微服务获取用户购买过当前专辑失败");
            }
            // 买过该专辑
            if (isPaidAlbum) {
                return albumTrackListVoPage;
            }
            // 没有买过该专辑
            List<AlbumTrackListVo> reallyShowAlbumTrackList = albumTrackListVoPage.getRecords().stream().map(albumTrackListVo -> {
                if (albumTrackListVo.getOrderNum() > tracksForFree) {
                    albumTrackListVo.setIsShowPaidMark(true);
                }
                return albumTrackListVo;
            }).collect(Collectors.toList());

            return albumTrackListVoPage.setRecords(reallyShowAlbumTrackList);
        }
    }



    // endregion ---------- end  根据专辑 id 查询专辑下声音列表且显示付费图标   ---------
}
