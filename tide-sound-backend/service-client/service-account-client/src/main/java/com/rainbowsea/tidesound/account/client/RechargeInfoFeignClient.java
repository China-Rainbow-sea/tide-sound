package com.rainbowsea.tidesound.account.client;

import com.rainbowsea.tidesound.account.client.impl.RechargeInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-account", fallback = RechargeInfoDegradeFeignClient.class )
public interface RechargeInfoFeignClient {


}