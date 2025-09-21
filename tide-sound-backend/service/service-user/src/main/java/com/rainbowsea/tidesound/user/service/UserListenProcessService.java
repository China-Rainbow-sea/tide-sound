package com.rainbowsea.tidesound.user.service;

import com.rainbowsea.tidesound.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 查询已经登录用户的声音【暂停】描述
     * 获取上次声音播放进度
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long trackId);

    /**
     * 更新MongoDB中用户声音的播放进度
     *
     * @param userListenProcessVo
     */
    void updateListenProcess(UserListenProcessVo userListenProcessVo);




    /**
     * 用户听最近一次专辑对应的声音
     *
     * @return
     */
    Map<Object, Object> getLatelyTrack();
}
