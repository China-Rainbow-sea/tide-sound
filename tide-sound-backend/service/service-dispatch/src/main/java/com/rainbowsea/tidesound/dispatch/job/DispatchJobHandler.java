package com.rainbowsea.tidesound.dispatch.job;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.dispatch.mapper.XxlJobLogMapper;
import com.rainbowsea.tidesound.model.dispatch.XxlJobLog;
import com.rainbowsea.tidesound.search.client.SearchFeignClient;
import com.rainbowsea.tidesound.user.client.UserInfoFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class DispatchJobHandler {

    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;


    /**
     * 更新排行榜
     */
    @XxlJob(value = "distroScheduleUpdateRankingHandler")
    public void distroScheduleUpdateRanking() {

        XxlJobLog xxlJobLog = new XxlJobLog();
        Long currentTime = System.currentTimeMillis();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startTime = simpleDateFormat.format(new Date());
        try {
            log.info("准备开始更新排行榜:{}", startTime);
            // 更新排行榜
            Result<Boolean> flagResult = searchFeignClient.preRankingToCache();
            Boolean aBoolean = flagResult.getData();
            Assert.notNull(aBoolean, "远程调用专辑微服务更新排行榜失败");


            long jobId = XxlJobHelper.getJobId();
            xxlJobLog.setJobId(jobId);
            if (aBoolean) {
                // 将这次定时任务执行的业务操作保存到
                xxlJobLog.setStatus(1);  // 如果更新成功 保存1  跟新失败保存0

            } else {
                xxlJobLog.setStatus(0);
                xxlJobLog.setError("远程查询出现了异常");
            }

        } finally {
            xxlJobLog.setTimes((int) (System.currentTimeMillis() - currentTime));     // 执行完任务的耗时
            xxlJobLogMapper.insert(xxlJobLog);
            log.info("排行榜信息更新完毕：耗时{}", System.currentTimeMillis() - currentTime);

        }
    }



    /**
     * 更新vip过期时间
     */

    @XxlJob(value = "distroScheduleUpdateVip")
    public void distroScheduleUpdateVip() {

        XxlJobLog xxlJobLog = new XxlJobLog();

        Long currentTime = System.currentTimeMillis();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startTime = simpleDateFormat.format(new Date());
        try {
            log.info("准备更新过期用户身份:{}", startTime);
            // 更新排行榜
            userInfoFeignClient.updateExpireVip();
            xxlJobLog.setJobId(XxlJobHelper.getJobId());
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
        } finally {
            xxlJobLog.setTimes((int) (System.currentTimeMillis() - currentTime));     // 执行完任务的耗时
            xxlJobLogMapper.insert(xxlJobLog);
            log.info("vip过期身份信息更新完毕：耗时{}", System.currentTimeMillis() - currentTime);

        }
    }

}