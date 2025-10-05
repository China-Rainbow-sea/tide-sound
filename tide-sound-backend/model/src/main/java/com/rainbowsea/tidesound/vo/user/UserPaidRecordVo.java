package com.rainbowsea.tidesound.vo.user;

import lombok.Data;

import java.util.List;

@Data
public class UserPaidRecordVo {

    private String orderNo;
    private Long userId;
    private String itemType;  // 付款项的类型
    private List<Long> itemIdList;  // 付款项购买的商品id [专辑id "多"个声音id 套餐id]
}