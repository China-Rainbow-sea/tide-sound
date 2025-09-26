package com.rainbowsea.tidesound.search.listener;

import com.rainbowsea.tidesound.search.service.impl.ItemServiceImpl;
import org.redisson.api.RBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Description: 利用springBoot的监听器机制完成对分布式布隆数据的同步
 */
//@Component(没用的必须是在: src/main/resources/META-INF/spring.factories 配置信息才有效)
// 注解是--扫描机制 spi机制 手动放进去 listener一定要用spi机制是在: src/main/resources/META-INF/spring.factories 配置信息才有效)
// 项目最终的选择
public class BloomFilterListener implements SpringApplicationRunListener {


    Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * 该方法springboot在启动过程（就包含了spring容器中应用定义的Bean对象都创建完毕）中回调
     *
     * @param context   the application context.
     * @param timeTaken the time taken to start the application or {@code null} if unknown
     *                  <p>
     *                  started方法在容器启动的过程中调用两次。
     *                  第一次是springcloud的组件调用的。  ConfigurableApplicationContext spring容器里面是没有应用中定义好的Bean对象
     *                  第二次是springboot组件调用的。  ConfigurableApplicationContext spring容器里面才有应用中定义好的Bean对象
     */
    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {


        boolean containsBean = context.containsBean("rBloomFilter");

        // 这里是第二次才会加载 Spring boot 的组件，所以需要先判断一下, rBloomFilter 是否已经加载完毕了
        if (containsBean) {
            // 1.加载完毕，才能从spring容器中获取到布隆过滤器的Bean对象
            RBloomFilter rBloomFilter = context.getBean("rBloomFilter", RBloomFilter.class);

            // 判断 rBloomFilter 布隆过滤器(数据是否同步上了),已经同步了该数据到布隆当中了，就不要在创建了,
            // 没有同步上，说明是第一次才创建
            if (rBloomFilter.count() == 0) {
                // 2.从spring容器中获取应用的Bean对象
                ItemServiceImpl itemServiceImpl = context.getBean("itemServiceImpl", ItemServiceImpl.class);

                // 3.获取数据
                List<Long> albumInfoIdList = itemServiceImpl.getAlbumInfoIdList();

                // 4.将数据放到布隆过滤器中
                for (Long albumId : albumInfoIdList) {
                    rBloomFilter.add(albumId);
                }
                // 5.布隆过滤器元素是否同步进去
                logger.info("分布式布隆过滤器的元素个数：" + rBloomFilter.count());
            }

        }

    }
}
