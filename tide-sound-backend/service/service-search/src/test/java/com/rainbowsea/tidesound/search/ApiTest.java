package com.rainbowsea.tidesound.search;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.rainbowsea.tidesound.search.temp.client.HelloFeignClient;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApiTest {


    @Autowired
    private HelloFeignClient helloFeignClient;



    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter rBloomFilter;

    @Test
    public  void  testApi5(){
        System.out.println(redissonClient+"redisson客户端");
        System.out.println(rBloomFilter+"redisson的布隆过滤器");
    }



    /**
     * 本地布隆的使用
     * 引入guava包
     * Funnel<? super T> funnel：逻辑概念：通道未来存放元素
     * int expectedInsertions,期望插入元素的个数
     * double fpp：误判率
     */
    @Test
    public void testApi3() {
        // 1.得到funnel对象
        Funnel<Long> longFunnel = Funnels.longFunnel();

        // 2.创建布隆过滤器对象
        BloomFilter<Long> longBloomFilter = BloomFilter.create(longFunnel, 1000000, 0.01);
//        BloomFilter<Long> longBloomFilter = BloomFilter.create(longFunnel);


        // 3.将元素放到布隆过滤器中
        boolean data1 = longBloomFilter.put(1L);
        boolean data2 = longBloomFilter.put(100L);
        boolean data3 = longBloomFilter.put(1000L);
        boolean data4 = longBloomFilter.put(10000L);

        System.out.println(data1);
        System.out.println(data2);
        System.out.println(data3);
        System.out.println(data4);
        System.out.println(longBloomFilter.mightContain(99L));


    }


    @Test
    public void testApi1(){
        String result = helloFeignClient.sayHelloWithParam("哈喽");
        System.out.println(result);
    }
}
