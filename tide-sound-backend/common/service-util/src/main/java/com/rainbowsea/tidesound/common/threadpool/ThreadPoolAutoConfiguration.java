package com.rainbowsea.tidesound.common.threadpool;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义线程池
 * 自定义线程池对象
 */

@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolAutoConfiguration {


    // 获取到对应微服务的模块名，作为线程名
    @Value("${spring.application.name}")
    private String appName;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 自定义线程池对象
     * <p>
     * int corePoolSize: 核心线程数：执行任务
     * int maximumPoolSize,：最大线程数：执行任务（任务比较多 核心不够 队列也满了）
     * long keepAliveTime, 存活时间：队列没有任务  线程池线程要在一定时间之后 发现还没任务 淘汰一部分线程 保留核心线程
     * TimeUnit unit, 时间单位
     * BlockingQueue<Runnable> workQueue, 阻塞队列：存放任务的
     * ThreadFactory threadFactory,  线程工厂：自定义线程的
     * RejectedExecutionHandler handler：拒绝策略 有四种（拒绝任务的） 自定义
     * <p>
     * 阻塞队列是一个接口 该接口下的实现类非常多：
     * 常用的阻塞队列的实现类就俩：
     * ArrayBlockQueue:
     * LinkedBlockQueue:
     * <p>
     * <p>
     * ArrayBlockQueue和LinkedBlockQueue区别：
     * 区别一：底层数据结构不一样
     * ArrayBlockQueue：底层使用数组这种数据结构
     * LinkedBlockQueue：底层使用的是链表数据结构
     * <p>
     * 区别二：加锁方式不一样
     * ArrayBlockQueue：将对整个队列加锁（锁的粒度大，性能低）
     * LinkedBlockQueue：对队列中操作的位置加锁（锁的粒度小，性能高）
     * <p>
     * 区别三：风险问题。
     * ArrayBlockQueue：使用的时候一定要传一个参数作为队列的大小.
     * LinkedBlockQueue:使用的时候可以不用传入一个参数作为队列大小,那么队列大小就是无限大 也即无界【Integer.MAX_VALUE 2^31-1】
     * LinkedBlockQueue：由于是无界的，所以提交的任务可能就会很多 如果很多的任务要创建很多比较大的对象 那么oom的风险就会比较大。因此在使用的时候给一个大小限制。
     */

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(@Autowired ThreadPoolProperties threadPoolProperties) {

        // 对于核心线程数的设定：
        // 任务类型
        // 如果是cpu密集型任务[算素数。]：线程数=cpu的核数+1;
        // 如果是io密集型任务[查库 查网络 读写操作...]： 线程数=2cpu的核数;

        // 实际核心线程数给多少一定要要通过压测某个接口，得到一个准确值。因为理论算出来的线程数往往都是会偏小。


//        int i = Runtime.getRuntime().availableProcessors();
        int corePoolSize = threadPoolProperties.getCorePoolSize();  // 压测得到
        int maximumPoolSize = threadPoolProperties.getMaxPoolSize(); // 压测给到
        long ttl = 30l;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        ;
        LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>(threadPoolProperties.getQueueSize());

        // 自定义线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {

            AtomicInteger atomicInteger = new AtomicInteger(0);  //

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("my-customer-【" + appName + "】-" + atomicInteger.incrementAndGet());
                return thread;
            }
        };

        // 自定义拒绝策略
        RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                logger.error("提交给线程池的任务：{}，被拒绝了,请及时关注!", r);
                throw new RejectedExecutionException("提交给线程池的任务被拒绝了");
            }
        };


        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                ttl,
                timeUnit,
                blockingQueue,
                threadFactory,
                rejectedExecutionHandler
        );
        return threadPoolExecutor;

    }


}
