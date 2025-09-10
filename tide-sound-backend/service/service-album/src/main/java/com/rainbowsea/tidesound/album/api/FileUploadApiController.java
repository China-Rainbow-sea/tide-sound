package com.rainbowsea.tidesound.album.api;

import com.rainbowsea.tidesound.common.minio.service.FileUploadService;
import com.rainbowsea.tidesound.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {


    @Autowired
    private FileUploadService fileUploadService;

    // http://localhost:8500/api/album/fileUpload

    @Operation(summary = "图片文件的上传")
    @PostMapping("/fileUpload")
    public Result fileUpload(MultipartFile file) { // 注意：这里参数名必须是 file 因为前端写死了无法改变


        String picUrl = fileUploadService.fileUploadService(file);

        //System.out.println(file.getOriginalFilename()); // 获取到的文件名是被加密过后的
        return Result.ok(picUrl);

    }

}
