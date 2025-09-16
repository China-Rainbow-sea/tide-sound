package com.rainbowsea.tidesound.search.api;

import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.search.service.ItemService;
import com.rainbowsea.tidesound.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "专辑详情管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class itemApiController {

	@Autowired
	private ItemService itemService;


	@Autowired
	private SearchService searchService;


	// Request URL: http://192.168.200.1:8500/api/search/albumInfo/channel/1
	@GetMapping("/channel/{c1Id}")
	@Operation(summary = "频道页数据展示")
	public Result channel(@PathVariable(value = "c1Id") Long c1Id) {

		// 是一个7个Map的集合
		// Map的key:baseCategory--->三级分类对象
		// Map的key:list---该三级分类下热度值比较高的6张专辑【List】
		List<Map<String, Object>> mapList = searchService.channel(c1Id);
		return Result.ok(mapList);
	}


	@DeleteMapping("/batchAlbumOffSale")
	@Operation(summary = "专辑批量的下架")
	public Result batchAlbumOffSale() {

		itemService.batchAlbumOffSale();
		return Result.ok();
	}



	/**
	 * 批量上架专辑到es中
	 * TODO(定时从数据库中将所有的专辑查询到 然后同步给es)
	 */

	@PostMapping("/batchAlbumOnSale")
	@Operation(summary = "专辑的批量上架")
	public Result batchAlbumOnSale() {
		for (int i = 1; i <= 1577; i++) {
			itemService.albumOnSale((long) i);
		}
		return Result.ok();
	}



	@DeleteMapping("/albumOffSale/{albumId}")
	@Operation(summary = "专辑的下架")
	public Result albumOffSale(@PathVariable(value = "albumId") Long albumId) {
		itemService.albumOffSale(albumId);
		return Result.ok();
	}



	@PostMapping("/albumOnSale/{albumId}")
	@Operation(summary = "专辑的上架")
	public Result albumOnSale(@PathVariable(value = "albumId") Long albumId) {
		itemService.albumOnSale(albumId);  // Tomcat线程:任意一个web请求来了之后,tomcat线程池就会用它内部初始好的线程来执行web请求。 10个线程 200个线程
		return Result.ok();
	}





}

