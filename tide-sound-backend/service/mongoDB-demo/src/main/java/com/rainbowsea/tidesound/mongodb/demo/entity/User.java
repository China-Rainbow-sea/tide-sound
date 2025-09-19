package com.rainbowsea.tidesound.mongodb.demo.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 */

@Document(value = "User")
@Data
public class User {

    @Id   //文档主键_id
    private ObjectId id;
    private String name;
    private Integer age;
    private String email;
    private Date createDate;
}
