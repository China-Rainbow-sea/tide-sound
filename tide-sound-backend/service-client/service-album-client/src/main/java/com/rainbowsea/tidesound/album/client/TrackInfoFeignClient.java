package com.rainbowsea.tidesound.album.client;

import com.rainbowsea.tidesound.album.client.impl.TrackInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = TrackInfoDegradeFeignClient.class,
contextId = "trackInfoFeignClient")
public interface TrackInfoFeignClient {

}