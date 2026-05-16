package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    private static final String BUCKET_NAME = "dragonfly-blog";
    private static final String REGION = "cn-beijing";
    private static final String RAW_VIDEO_PATH = "video/raw/";
    private static final String TRANSCODED_VIDEO_PATH = "video/transcoded/";

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

        if (isImage) {
            String objectName = "images/" + fileName;
            String url = AliOssUtil.uploadFile(objectName, file.getInputStream());
            return Result.success(url);
        } else {
            // 1. 上传原始视频到 raw 目录（触发工作流）
            String rawObjectName = RAW_VIDEO_PATH + fileName;
            String rawUrl = AliOssUtil.uploadFile(rawObjectName, file.getInputStream());

            // 2. 工作流会自动转码，转码后的文件名（.mp4）
            String transcodedFileName = fileName.replace(extension, ".mp4");
            String transcodedObjectName = TRANSCODED_VIDEO_PATH + transcodedFileName;
            String transcodedUrl = "https://" + BUCKET_NAME + ".oss-" + REGION + ".aliyuncs.com/" + transcodedObjectName;

            System.out.println("原始视频: " + rawUrl);
            System.out.println("转码后视频(约1-2分钟后可用): " + transcodedUrl);

            // 3. 直接返回转码后的URL（工作流完成后就能访问）
            return Result.success(transcodedUrl);
        }
    }
}