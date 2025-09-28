package com.rainbowsea.tidesound.cdc.entity;

import lombok.Data;

import javax.persistence.Column;

/**
 * CdcEntity主要为了映射对应表（监听的）中的字段
 */

@Data
public class CdcEntity {



    @Column(name = "id")
    private  Long  id;  // 我们只需要监听到表中的id字段。所以只用定义一个属性就可以



}
