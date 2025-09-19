package com.rainbowsea.tidesound.mongodb.demo;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.rainbowsea.tidesound.mongodb.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

/**
 */

@SpringBootTest
public class ApiTest {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 插入文档
     */
    @Test
    public void testApi1() {
        User user = new User();
        user.setAge(20);
        user.setName("test");
        user.setEmail("test@qq.com");
        mongoTemplate.insert(user);
        System.out.println(user);

        User user1 = new User();
        user1.setAge(21);
        user1.setName("abc");
        user1.setEmail("abc@qq.com");
        mongoTemplate.insert(user1);
        System.out.println(user1);

    }

    //查询所有
    @Test
    public void testFindUser() {
        List<User> userList = mongoTemplate.findAll(User.class);
        System.out.println(userList);
    }


    //根据id查询
    @Test
    public void testFindUserById() {
        User user = mongoTemplate.findById("68cd40bc40ecaa59afec25e5", User.class);
        System.out.println(user);
    }


    //修改
    @Test
    public void testUpdateUser() {

        // 1.创建条件对象（where  is  and ）
        Criteria criteria = Criteria.where("_id").is("67da82c4068e38224ad486d9");
        // 2.创建查询对象（将条件对象放进去）
        Query query = new Query(criteria);

        // 3.创建修改对象
        Update update = new Update();
        update.set("name", "zhangsan");
        update.set("age", 99);
        // 4.开始修改
        UpdateResult result = mongoTemplate.upsert(query, update, User.class);
        long count = result.getModifiedCount();
        System.out.println(count);
    }

    //删除
    @Test
    public void testRemove() {
        Criteria criteria = Criteria.where("_id").is("67da82c4068e38224ad486d9");
        Query query = new Query(criteria);
        DeleteResult result = mongoTemplate.remove(query, User.class);
        long count = result.getDeletedCount();
        System.out.println(count);
    }


    //条件查询 and
    @Test
    public void findUserList() {

        Criteria criteria = Criteria.where("name").is("test").and("age").is(20);
        Query query = new Query(criteria);

        List<User> userList = mongoTemplate.find(query, User.class);
        System.out.println(userList);
    }


    //分页查询
    @Test
    public void findUsersPage() {
        Query query = new Query();
        //先查询总记录数
        long count = mongoTemplate.count(query, User.class);
        System.out.println(count);
        //分页+排序
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("age")));

        query.with(pageRequest);
        //  分页查询
        List<User> list = mongoTemplate.find(query, User.class);
        System.out.println(list);
    }

}