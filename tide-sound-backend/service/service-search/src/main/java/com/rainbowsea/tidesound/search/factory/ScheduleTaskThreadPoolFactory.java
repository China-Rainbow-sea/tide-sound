package com.rainbowsea.tidesound.search.factory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *  ScheduleTaskThreadPoolFactory 能够直接执行任务
 */
public class ScheduleTaskThreadPoolFactory {

    static ScheduledExecutorService scheduledExecutorService = null;


    // 加载ScheduleTaskThreadPoolFactory类的时候 提前将定时任务的线程池对象创建出来
    static {

        scheduledExecutorService = Executors.newScheduledThreadPool(2);
    }


    /**
     * 定义实例对象出来（单例设计模式） 饿汉式
     * 懒汉式
     * 饿汉式
     * double  check
     * 枚举，，，
     */

    private static ScheduleTaskThreadPoolFactory INSTANCE = new ScheduleTaskThreadPoolFactory();


    /**
     * 提供一个方法 获取实例对象
     */

    public static ScheduleTaskThreadPoolFactory getINSTANCE() {

//        return INSTANCE;
        return INSTANCE;   // i++
    }


    /**
     * 私有化构造器
     */
    private ScheduleTaskThreadPoolFactory() {

    }

    /**
     * 提供一个方法  接外部提交的任务
     */

    public void execute(Runnable runnable, Long ttl, TimeUnit timeUnit) {
        scheduledExecutorService.schedule(runnable, ttl, timeUnit);
    }


    /**
     * 提供一个方法 计算时间差
     */

    public Long diffTime(Long currentTime) {


        LocalDate localDate = LocalDate.now().plusDays(7);

        LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalTime.of(2, 0, 0));


        long time = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();


        Long diffTime = currentTime - time;

//        Long diffTime =time-currentTime;
        long absDiffTime = Math.abs(diffTime);

        return absDiffTime;

    }

}
