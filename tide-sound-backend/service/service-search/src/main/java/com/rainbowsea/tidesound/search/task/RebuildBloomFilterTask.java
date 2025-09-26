package com.rainbowsea.tidesound.search.task;


import com.rainbowsea.tidesound.search.service.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 重建布隆过滤器
 */
@Component
@Slf4j
public class RebuildBloomFilterTask {

    @Autowired
    private ItemService itemService;


    /**
     * 一共有7位组成  用到的会有6位
     * 秒 分 时  日 月 周 （年）
     * 注意：日和周不能同时出现  如果写日 周就不要具体？
     * <p>
     * <p>
     * 字段	允许值	特殊字符
     * 秒	0-59	, - * /
     * 分	0-59	, - * /
     * 时	0-23	, - * /
     * 日	1-31	, - * / ?
     * 月	1-12 或 JAN-DEC	, - * /
     * 星期	0-7 或 SUN-SAT	, - * / ?
     * <p>
     * 注意：0 和 7 均表示周日。
     * 注意：日与星期字段冲突：如果同时指定了日和星期字段，Spring会触发两者的条件（可能导致意外行为）。建议用?忽略其中一个字段。
     * <p>
     * 特殊字符说明
     * 字符	含义	示例
     * *	所有值（任意时刻）	0 * * * * * 每分钟执行
     * ?	忽略该字段（仅用于日或星期字段）	0 0 0 ? * MON 每周一执行
     * -	范围	0 0 9-17 * * * 9点到17点每小时执行
     * ,	多个值	0 0 8,12,18 * * * 每天8点、12点、18点执行
     * /	步长	0 0/5 * * * * 每5分钟执行一次
     * <p>
     * * 每7天在凌晨两点执行这个定时任务完成布隆重建
     */


//    @Scheduled(fixedDelay = 1000)
//    @Scheduled(cron = "0 0 2 */7 * ?")   // 定时效果  线上 每 7天 重建布隆过滤器(更新分布式布隆过滤器数据)
//    @Scheduled(cron = "10/* * * * * *")  // 测试环境：每 10 s 重建布隆过滤器(更新分布式布隆过滤器数据)
//    @Scheduled(cron = "*/10 * * * * *")  // 测试环境：每 10 s 重建布隆过滤器(更新分布式布隆过滤器数据)
    public void rebuildBloomFilter() {
        Boolean aBoolean = itemService.rebuildBloomFilter();
        log.info("分布式布隆重建：{}", aBoolean ? "success" : "fail");
    }


}
