package com.rainbowsea.tidesound.user.closeorder.delay;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 支付超时，订单关闭 利用 java 编程的 JUC 当中的 DelayQueue 延时队列
 */
public class DelayOrderTask implements Delayed {

    private final String orderId;       // 订单ID
    private final long expireTime;      // 过期时间（时间戳）

    public DelayOrderTask(String orderId, long delaySeconds) {
        this.orderId = orderId;
        this.expireTime = System.currentTimeMillis() + delaySeconds * 1000;
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        long remaining = expireTime - System.currentTimeMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        // 到期时间早的任务排在队列前面，确保调用 take() 方法时，优先取出最早到期的任务
        return Long.compare(this.expireTime, ((DelayOrderTask) o).expireTime);
    }

    public String getOrderId() {
        return orderId;
    }
}
