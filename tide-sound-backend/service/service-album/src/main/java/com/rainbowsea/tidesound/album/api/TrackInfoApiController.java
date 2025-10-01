package com.rainbowsea.tidesound.album.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rainbowsea.tidesound.album.service.TrackInfoService;
import com.rainbowsea.tidesound.common.login.annotation.TingshuLogin;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.common.util.AuthContextHolder;
import com.rainbowsea.tidesound.query.album.TrackInfoQuery;
import com.rainbowsea.tidesound.vo.album.AlbumTrackListVo;
import com.rainbowsea.tidesound.vo.album.TrackInfoVo;
import com.rainbowsea.tidesound.vo.album.TrackListVo;
import com.rainbowsea.tidesound.vo.album.TrackStatVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;



	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/getTrackStatVo/36362
	@GetMapping(value = "/getTrackStatVo/{trackId}")
	@Operation(summary = "根据声音id查询声音的统计信息")
	public Result getTrackStatVo(@PathVariable(value = "trackId") Long trackId) {

		TrackStatVo trackStatVo = trackInfoService.getTrackStatVo(trackId);
		return Result.ok(trackStatVo);

	}



	// 专辑的详情接口：
	// 1.用户可登录之后 来访问
	// 2.也可以不登录直接访问
	@GetMapping("/findAlbumTrackPage/{albumId}/{pn}/{pz}")
	@Operation(summary = "根据专辑id查询该专辑下声音列表且显示付费图标")
	@TingshuLogin(required = false)
	public Result findAlbumTrackPage(@PathVariable(value = "albumId") Long albumId,
									 @PathVariable(value = "pn") Long pn,
									 @PathVariable(value = "pz") Long pz) {


		IPage<AlbumTrackListVo> albumTrackListVoPage = new Page<>(pn, pz);
		albumTrackListVoPage = trackInfoService.findAlbumTrackPage(albumTrackListVoPage, albumId);
		return Result.ok(albumTrackListVoPage);
	}




	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/removeTrackInfo/51943
	@Operation(summary = "根据声音id 删除声音信息")
	@TingshuLogin
	@DeleteMapping("/removeTrackInfo/{trackId}")
	public Result removeTrackInfo(@PathVariable(value = "trackId") Long trackId) {
		trackInfoService.removeTrackInfo(trackId);
		return Result.ok();
	}

	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/updateTrackInfo/51943
	@TingshuLogin
	@PutMapping("/updateTrackInfo/{trackId}")
	@Operation(summary = "实现修改声音专栏的信息")
	public Result updateTrackInfo(@PathVariable(value = "trackId") Long trackId,
								  @RequestBody TrackInfoVo trackInfoVo) {

		trackInfoService.updateTrackInfo(trackId, trackInfoVo);

		return Result.ok();
	}


	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/getTrackInfo/51943
	@TingshuLogin
	@Operation(summary = "根据声音id查询声音对象")
	@GetMapping("/getTrackInfo/{trackId}")
	public Result getTrackInfo(@PathVariable(value = "trackId") Long trackId) {
		return Result.ok(trackInfoService.getById(trackId));
	}


	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/findUserTrackPage/1/10
	@PostMapping("/findUserTrackPage/{pn}/{pz}")
	@Operation(summary = "分页展示用户创作的声音列表")
	@TingshuLogin
	public Result findUserTrackPage(@PathVariable(value = "pn") Long pn,
									@PathVariable(value = "pz") Long pz,
									@RequestBody TrackInfoQuery trackInfoQuery) {
		// 1.构建分页对象(对返回给前端的数据进行分页)
		IPage<TrackListVo> pageParam = new Page<>(pn, pz);

		trackInfoQuery.setUserId(AuthContextHolder.getUserId());
		// 2.将分页对象传进去(未来自动给Page对象的records属性赋值：list)
		pageParam = trackInfoService.findUserTrackPage(pageParam, trackInfoQuery);

		// 3.将分页对象返回出去
		return Result.ok(pageParam);
	}


	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/uploadTrack
	// 将本地的声音存放到腾讯云vod中

	@PostMapping("/uploadTrack")
	@Operation(summary = "上传声音")
	public Result uploadTrack(MultipartFile file) {
		// 文件在vod中的id mediaFileId
		// 文件在vod中的地址 mediaUrl
		Map<String, Object> map = trackInfoService.uploadTrack(file);

		return Result.ok(map);

	}


	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/saveTrackInfo
	@PostMapping("/saveTrackInfo")
	@TingshuLogin
	@Operation(summary = "保存声音到对应的专辑当中去")
	public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {

		trackInfoService.saveTrackInfo(trackInfoVo);

		return Result.ok();
	}


	// Request URL: http://192.168.200.1:8500/api/album/trackInfo/findUserTrackPaidList/48241

	@Operation(summary = "分集展示要买的声音列表")
	@TingshuLogin
	@GetMapping("/findUserTrackPaidList/{currentTrackId}")
	public Result findUserTrackPaidList(@PathVariable(value = "currentTrackId") Long currentTrackId) {

		List<Map<String, Object>> mapList = trackInfoService.findUserTrackPaidList(currentTrackId);
		return Result.ok(mapList);

	}

}

