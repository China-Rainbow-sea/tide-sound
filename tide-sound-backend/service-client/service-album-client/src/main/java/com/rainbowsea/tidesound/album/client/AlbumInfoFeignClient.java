package com.rainbowsea.tidesound.album.client;

import com.rainbowsea.tidesound.album.client.impl.AlbumInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = AlbumInfoDegradeFeignClient.class
,contextId = "albumInfoFeignClient" )
public interface AlbumInfoFeignClient {

}