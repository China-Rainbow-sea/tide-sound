package com.rainbowsea.tidesound.album.temp;

import com.rainbowsea.tidesound.album.mapper.AlbumInfoMapper;
import com.rainbowsea.tidesound.common.result.Result;
import com.rainbowsea.tidesound.model.album.AlbumInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试时间类型上的问题
 */

@RestController
@RequestMapping("/v1")
public class TempController {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;


    @GetMapping("/testDate")
    public Result getAlbumInfo() {

        AlbumInfo albumInfo = albumInfoMapper.selectById(1602);

        return Result.ok(albumInfo);

    }
}
