package com.xxl.job.executor.glue;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author:huzhongkui
 * Date: 2025-02-15 星期六 00:05:16
 * Description:
 */

@Service
public class GlueService {


    public void testGlue() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("glue默认任务开始执行时间为：" + simpleDateFormat.format(new Date()));
    }
}
