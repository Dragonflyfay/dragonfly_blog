package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.awt.font.MultipleMaster;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/24 20:18
 */
@RestController
public class FileUploadController {
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        //把文件的内容保存到本地的磁盘上
        String originalFilename = file.getOriginalFilename();
        //保证文件的名称唯一
        String fileName = UUID.randomUUID().toString()+originalFilename.substring(originalFilename.lastIndexOf("."));
        //file.transferTo(new File("C:\\Users\\15041\\Desktop\\File\\"+fileName));
        String url=AliOssUtil.uploadFile(fileName,file.getInputStream());
        return Result.success(url);//上传到服务器之后的URL地址

    }
}
