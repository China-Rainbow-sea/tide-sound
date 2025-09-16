package com.rainbowsea.tidesound.search;


import com.rainbowsea.tidesound.search.temp.client.HelloFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApiTest {


    @Autowired
    private HelloFeignClient helloFeignClient;


    @Test
    public void testApi1(){
        String result = helloFeignClient.sayHelloWithParam("哈喽");
        System.out.println(result);
    }
}
