package com.rainbowsea.tidesound.es.demo.repository;


import com.rainbowsea.tidesound.es.demo.entity.Person;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PersonRepository extends CrudRepository<Person,Long> {


    /**
     * 查询地址包含市的模糊查询
     */

    List<Person> findByAddressContains(String address);



    /**
     * 查询名字叫张三且年龄是18的文档信息
     */

    Person findByNameEqualsAndAgeEquals(String name, Integer age);



    /**
     * 查询名字包含张三的文档信息(定义一个方法)
     * findXXX 或者getXXX 未来data底层才会用代理对象做实现 反之 没有实现
     */

    // getxxx 也是可以的
    Person getByNameContains(String name);

    // findXXX 也是可以的
    Person findByNameContains(String name);
}
