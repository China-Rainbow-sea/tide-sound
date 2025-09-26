package com.rainbowsea.tidesound.search.runners;


import com.rainbowsea.tidesound.search.service.impl.ItemServiceImpl;
import org.redisson.api.RBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 项目启动 在 spring 容器启动完毕的时候调到 执行如下的 run () 方法 的任务
 */
//@Component  // --项目不用
public class BloomFilterRunners implements ApplicationRunner, CommandLineRunner, ApplicationContextAware {

    private ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        this.applicationContext = applicationContext;
    }
    Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * run方法在spring容器启动完毕的时候调到
     * java  -jar  search.jar  --server.port=8010 output=D:/app/serach/log/1.log
     * <p>
     * ApplicationArguments：回调时候的参数。
     * --:可选参数： 有默认值不会出错
     * 不加-- 必选参数
     *
     * @param args incoming application arguments
     * @throws Exception 先调用
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 可以检验命令行输入的参数。
//        Set<String> optionNames = args.getOptionNames();
//        for (String optionName : optionNames) {
//            System.out.println("命令行中输入的可选参数名：" + optionName + "值：" + args.getOptionValues(optionName));
//        }
//        for (String nonOptionArg : args.getNonOptionArgs()) {
//            System.out.println("命令行中输入的非可选参数名：" + nonOptionArg + "值：" + args.getOptionValues(nonOptionArg));
//        }


        // 1.从spring容器中获取到布隆过滤器的Bean对象
        RBloomFilter rBloomFilter = applicationContext.getBean("rBloomFilter", RBloomFilter.class);

        // 2.从spring容器中获取应用的Bean对象
        ItemServiceImpl itemServiceImpl = applicationContext.getBean("itemServiceImpl", ItemServiceImpl.class);

        // 3.获取数据
        List<Long> albumInfoIdList = itemServiceImpl.getAlbumInfoIdList();

            // 4.将数据放到布隆过滤器中
        for (Long albumId : albumInfoIdList) {
            rBloomFilter.add(albumId);
        }

        // 5.布隆过滤器元素是否同步进去
        logger.info("分布式布隆过滤器的元素个数：" + rBloomFilter.count());


    }

    /**
     * 后调用
     *
     * @param args incoming main method arguments
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {

    }


}
