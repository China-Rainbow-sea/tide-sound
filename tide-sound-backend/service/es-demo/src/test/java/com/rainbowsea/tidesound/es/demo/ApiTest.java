package com.rainbowsea.tidesound.es.demo;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.rainbowsea.tidesound.es.demo.entity.Person;
import com.rainbowsea.tidesound.es.demo.repository.PersonRepository;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SpringBootTest     // 可以使用到es-demo应用的组件
public class ApiTest {


    @Autowired
    private ElasticsearchClient elasticsearchClient;


    /**
     * 查询 my_index 索引库中标题中有华为的文档（聚合）方式二
     */
    @SneakyThrows
    @Test
    public void testApi8() {
        SearchResponse<Object> response = elasticsearchClient.search(b -> b.index("my_index").query(qb -> qb.match(mqb -> mqb.field("title").query("华为"))), Object.class);

        // 3.获取数据
        List<Hit<Object>> hits = response.hits().hits();
        for (Hit<Object> hit : hits) {
            Object source = hit.source();
            System.out.println(source);
        }

    }


    /**
     * 查询 my_index 索引库中标题中有华为的文档（聚合）
     */
    @SneakyThrows
    @Test
    public void testApi7() {

        // 选择该方式
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index("my_index").query(b -> b.match(mb -> mb.field("title").query("华为")));
        // 1.创建SearchRequest对象
        SearchRequest searchRequest = builder.build();
        System.out.println("发送给es的dsl语句:" + searchRequest.toString());


        // 2.查询es 得到响应
        SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);


        // 3. 获取数据
        List<Hit<Object>> hits = response.hits().hits();
        for (Hit<Object> hit : hits) {
            Object source = hit.source();
            System.out.println(source);
        }

    }


    @Autowired
    private PersonRepository personRepository;


    @Test
    public void testDele() {
        personRepository.deleteAll();
    }

    /**
     * 查询地址包含市的模糊查询
     */
    @Test
    public void testApi6() {
        List<Person> list = personRepository.findByAddressContains("市");
        for (Person person : list) {
            System.out.println(person);
        }

    }


    /**
     * 查询名字叫张三且年龄是18的文档信息
     */
    @Test
    public void testApi5() {

        Person person = personRepository.findByNameEqualsAndAgeEquals("张三", 18);
        System.out.println(person);


    }


    /**
     * 查询名字叫张三的文档信息
     */
    @Test
    public void testApi4() {

//        Person person = personRepository.findByNameContains("张三");

        Person person = personRepository.getByNameContains("张三");

        System.out.println(person);


    }


    /**
     * 查询文档id=1的文档信息
     */
    @Test
    public void testApi3() {
        Optional<Person> person = personRepository.findById(1L);
        System.out.println(person.get());
    }


    /**
     * 查询person索引库下的所有文档
     */
    @Test
    public void testApi2() {
        Iterable<Person> all = personRepository.findAll();
        for (Person person : all) {
            System.out.println(person);

        }
    }


    /**
     * 保存文档
     */

    @Test
    public void testApi1() {

        List<Person> people = new ArrayList<>();


        // 文档对象创建
        Person person = new Person();
        person.setId(1L);
        person.setName("张三");
        person.setAge(18);
        person.setAddress("北京市昌平区");

        Person person1 = new Person();
        person1.setId(2L);
        person1.setName("李四");
        person1.setAge(28);
        person1.setAddress("上海市松江区");


        Person person2 = new Person();
        person2.setId(3L);
        person2.setName("王五");
        person2.setAge(38);
        person2.setAddress("深圳市宝安区");


        Person person3 = new Person();
        person3.setId(4L);
        person3.setName("赵六");
        person3.setAge(48);
        person3.setAddress("武汉市洪山区");

        people.add(person);
        people.add(person1);
        people.add(person2);
        people.add(person3);

        personRepository.saveAll(people);


    }

}
