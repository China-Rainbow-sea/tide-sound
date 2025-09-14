package com.rainbowsea.tidesound.es.demo.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "person")  // 设置我们这个Bean对应映射创建的文档对象 person
@Data
public class Person {


    @Id
    private Long id;

    //  @Field(name = "该属性在文档当中的属性名", type = 属性类型, index = 是否添加索引, analyzer = "指明该属性使用的分词器")
    @Field(name = "name", type = FieldType.Text, index = true, analyzer = "ik_smart")
    private String name;

    @Field(name = "age",type = FieldType.Integer,index = true)
    private Integer age;

    @Field(name = "address",type = FieldType.Text,index =true,analyzer = "ik_smart")
    private String address;
}
