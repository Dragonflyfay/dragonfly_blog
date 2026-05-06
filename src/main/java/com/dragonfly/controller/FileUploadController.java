package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
public class FileUploadController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    ));

    private static final Set<String> ALLOWED_VIDEO_TYPES = new HashSet<>(Arrays.asList(
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/x-flv", "video/3gpp"
    ));

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Result.error("文件名无效");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            return Result.error("无法识别文件类型");
        }

        boolean isImage = ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
        boolean isVideo = ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase());

        if (!isImage && !isVideo) {
            return Result.error("不支持的文件类型，仅支持图片(jpg/png/gif/webp)和视频(mp4/mov/avi/flv/3gp)");
        }

        long maxSize = isVideo ? 500 * 1024 * 1024 : 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            String sizeLimit = isVideo ? "500MB" : "10MB";
            return Result.error("文件大小不能超过" + sizeLimit);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        String fileName = UUID.randomUUID().toString() + extension;

        String url = AliOssUtil.uploadFile(fileName, file.getInputStream());
        return Result.success(url);
    }
}
