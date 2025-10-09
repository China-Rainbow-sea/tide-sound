package com.xxl.job.executor.service.handler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author:huzhongkui
 * Date: 2025-02-15 星期六 10:21:46
 * Description:
 * Bean类型的方法模式（任意写一个方法 然后在该方法上使用xxl-job提供的一个注解 并且在该注解中执行方法处理器的名字）
 * Bean类型的类模式
 */

@Component
public class DemoHandler {

    @XxlJob("abc")
    public void executeTask() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("任务开始执行，时间是:" + simpleDateFormat.format(new Date()));

    }


    @XxlJob("childabc")
    public void executeTaskChild() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("子任务开始执行，时间是:" + simpleDateFormat.format(new Date()));

    }






}
