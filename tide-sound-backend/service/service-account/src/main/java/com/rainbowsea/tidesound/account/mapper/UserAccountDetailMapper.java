package com.rainbowsea.tidesound.account.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rainbowsea.tidesound.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountDetailMapper extends BaseMapper<UserAccountDetail> {

    @Select("  select * \n" +
            "   from user_account_detail\n" +
            "   where user_id = #{userId} and trade_type = '1204'\n" +
            "   and is_deleted = 0\n" +
            "   order by update_time desc")
    IPage<UserAccountDetail> findUserConsumePage(@Param("page") IPage<UserAccountDetail> page, @Param("userId") Long userId);



    @Select("  select * \n" +
            "   from user_account_detail\n" +
            "   where user_id = #{userId} and trade_type = '1201'\n" +
            "   and is_deleted = 0\n" +
            "   order by update_time desc")
    IPage<UserAccountDetail> findUserRechargePage(@Param("page") IPage<UserAccountDetail> page, @Param("userId") Long userId);



}
