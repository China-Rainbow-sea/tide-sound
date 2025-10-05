package com.rainbowsea.tidesound.order.mapper;

import com.rainbowsea.tidesound.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.validation.constraints.NotEmpty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {


    @Select("\n" +
            "SELECT  \n" +
            "count(*)\n" +
            "FROM  order_info  \n" +
            "INNER  JOIN   order_detail\n" +
            "ON  order_info.id= order_detail.order_id\n" +
            "WHERE order_info.user_id=#{userId} AND order_info.item_type=#{itemType} AND order_info.order_status='0901'   AND  order_detail.item_id=#{itemId}")
    Long getItemTypeAlbumAndVipIsPadding(@Param("userId") Long userId, @Param("itemType") String itemType, @Param("itemId") Long itemId);


    @Select("SELECT  \n" +
            "order_detail.item_id\n" +
            "FROM  order_info  \n" +
            "INNER  JOIN   order_detail\n" +
            "ON  order_info.id= order_detail.order_id\n" +
            "WHERE order_info.user_id=#{userId}  AND order_info.order_status='0901' AND order_info.item_type=#{itemType}  ")
    List<Long> getItemTypeTrackIsPadding(@Param("userId") Long userId, @Param("itemType") String itemType);


    @Update("update order_info set  order_info.order_status='0902' where order_info.order_no=#{orderNo} and  order_info.user_id=#{userId} and  order_info.order_status='0901'")
    int updateOrderStatus(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    @Update("update order_info set  order_info.order_status='0903' where order_info.order_no=#{orderNo} and  order_info.order_status='0901'")
    int cancelOrder(String orderNo);
}
