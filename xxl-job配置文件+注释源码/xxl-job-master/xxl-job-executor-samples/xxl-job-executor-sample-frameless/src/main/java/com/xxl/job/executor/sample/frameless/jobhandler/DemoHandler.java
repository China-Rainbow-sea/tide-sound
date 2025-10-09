package com.xxl.job.executor.sample.frameless.jobhandler;

import com.xxl.job.core.handler.IJobHandler;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author:huzhongkui
 * Date: 2025-02-15 星期六 10:29:26
 * Description: Bean类型的类模式
 */
public class DemoHandler extends IJobHandler {
    @Override
    public void execute() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Bean模式的类类型执行任务：" + simpleDateFormat.format(new Date()));
    }
}
