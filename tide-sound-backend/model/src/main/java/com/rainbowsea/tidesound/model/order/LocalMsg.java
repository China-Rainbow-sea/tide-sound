package com.rainbowsea.tidesound.model.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

/**
 * 本地数据表（保证）消息队列分布式数据的幂等性，就是保证消息队列的成功消费接受处理数据
 * 消息队列导致分布式事务，本地消息表 +定时任务；消息没有被消费，
 * 就让生产者一直发，直到被消费者消费掉消息为止。利用本地消息表进行一个标记。
 */

@TableName("t_local_msg")
@Data
public class LocalMsg {


    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "msg_content")
    private String msgContent;
    @TableField(value = "status")
    private Integer status;

    @TableField("create_time")
    private Date createTime;   //  Mon Sep 02 10:38:08 CST 2024

    @JsonIgnore // 不会参与序列化
    @TableField("update_time")
    private Date updateTime;


}
