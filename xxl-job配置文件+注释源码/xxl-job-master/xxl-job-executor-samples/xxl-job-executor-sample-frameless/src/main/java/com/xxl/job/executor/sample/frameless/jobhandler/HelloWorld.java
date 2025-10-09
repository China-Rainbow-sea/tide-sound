package com.xxl.job.executor.sample.frameless.jobhandler;

import com.xxl.job.core.handler.IJobHandler;

/**
 * Author:huzhongkui
 * Date: 2025-02-14 星期五 21:47:36
 * Description:
 */
public class HelloWorld extends IJobHandler {
    @Override
    public void execute() throws Exception {
        System.out.println("yyyyyy");
    }
}
