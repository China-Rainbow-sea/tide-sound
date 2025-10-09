package com.rainbowsea.tidesound.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.model.user.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbowsea.tidesound.vo.album.TrackStatMqVo;
import com.rainbowsea.tidesound.vo.user.UserCollectVo;
import com.rainbowsea.tidesound.vo.user.UserInfoVo;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {


    /**
     *  查询专辑的专辑信息
     *
     * @param albumId
     * @param trackId
     * @param trackStatType
     * @param count
     * @return
     */
    public TrackStatMqVo prepareTrackStatMqDto(Long albumId, Long trackId, String trackStatType, int count);



    /**
     * 微信小程序登录(微信登录)
     * @param code
     * @return
     */
    Map<String, Object> wxLogin(String code);

    /**
     * 向 Redis 获取第二个refreshToken 的 token 值(双token 的设计的情况下)
     * @return
     */
    Map<String, Object> getNewAccessToken();


    /**
     * 更新用户信息：用户头像，用户昵称
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);

    /**
     * 通过用户id，获取用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 查询用户买过当前专辑下的声音
     * @param userId
     * @param albumId
     * @return
     */
    Map<Long, String> getUserPaidAlbumTrack(Long userId, Long albumId);


    /**
     * 查询用户买过该专辑
     * @param userId
     * @param albumId
     * @return
     */
    Boolean getUserPaidAlbum(Long userId, Long albumId);


    /**
     * 收藏与取消收藏声音
     *
     * @param trackId
     * @return
     */
    Boolean collect(Long trackId);


    /**
     * 是否收藏声音
     *
     * @param trackId
     * @return
     */
    Boolean isCollect(Long trackId);

    /**
     * 是否订阅过专辑
     *
     * @param albumId
     * @return
     */
    Boolean isSubscribe(Long albumId);

    /**
     * 查询用户收藏过的声音列表分页展示
     *
     * @param pageParam
     * @return
     */
    IPage<UserCollectVo> findUserCollectPage(IPage<UserCollectVo> pageParam);

    /**
     * 更新vip过期时间
     */
    void updateExpireVip();
}
