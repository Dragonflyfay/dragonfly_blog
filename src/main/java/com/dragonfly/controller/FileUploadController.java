package com.dragonfly.controller;

import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.StsService;
import com.dragonfly.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
public class FileUploadController {

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return Result.error("文件名不能为空");
            }

            int lastDotIndex = originalFilename.lastIndexOf(".");
            String suffix = lastDotIndex >= 0 ? originalFilename.substring(lastDotIndex) : "";
            String fileName = UUID.randomUUID().toString() + suffix;

            AssumeRoleResponse stsResponse = StsService.getStsCredential();
            String accessKeyId = stsResponse.getCredentials().getAccessKeyId();
            String accessKeySecret = stsResponse.getCredentials().getAccessKeySecret();
            String securityToken = stsResponse.getCredentials().getSecurityToken();

            String url = AliOssUtil.uploadFileWithSts(
                    fileName,
                    file.getInputStream(),
                    accessKeyId,
                    accessKeySecret,
                    securityToken
            );

            return Result.success(url);
        } catch (IllegalStateException e) {
            return Result.error("服务配置错误：" + e.getMessage());
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
