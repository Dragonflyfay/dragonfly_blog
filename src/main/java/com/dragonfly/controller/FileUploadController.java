package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/2 0:07
 */
@RestController
public class FileUploadController {

    @PostMapping("/upload")
    public Result<String > upload(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String fileName= UUID.randomUUID().toString()+originalFilename.substring(originalFilename.lastIndexOf("."));
        String url= AliOssUtil.uploadFile(fileName,file.getInputStream());
        return Result.success(url);
    }
}
