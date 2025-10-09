package com.xxl.job.executor.expireTask;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Author:huzhongkui
 * Date: 2024-04-17 星期三 23:52:43
 * Description:
 */
public class expireTaskTest {

    // 模拟任务调度器
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // 模拟任务
    static class JobTask implements Runnable {
        private String jobName;
        private long scheduledTime;

        public JobTask(String jobName, long scheduledTime) {
            this.jobName = jobName;
            this.scheduledTime = scheduledTime;
        }

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if (currentTime > scheduledTime) {
                handleExpiredJob(jobName, currentTime - scheduledTime);
            } else {
                System.out.println("执行任务: " + jobName + ", 时间: " + new Date(scheduledTime));
            }
        }
    }

    // 处理过期任务
    private static void handleExpiredJob(String jobName, long delay) {
        System.out.println("任务: " + jobName + " 已过期, 过期时间: " + delay + "ms");

        // 模拟不同的过期策略
        String policy = "忽略";

        switch (policy) {
            case "忽略":
                System.out.println("忽略过期任务: " + jobName);
                break;
            case "立即执行一次":
                System.out.println("立即执行过期任务: " + jobName);
                // 立即执行任务
                new JobTask(jobName, System.currentTimeMillis()).run();
        }
    }

    public static void main(String[] args) {
        // 模拟任务调度
        long currentTime = System.currentTimeMillis();
        long scheduledTime = currentTime + 5000; // 任务计划在5秒后执行

        // 模拟任务调度器调度任务
        scheduler.schedule(new JobTask("测试任务", scheduledTime), 10, TimeUnit.SECONDS);

        // 关闭调度器
        scheduler.shutdown();
    }
}
