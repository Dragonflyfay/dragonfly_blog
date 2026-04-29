package com.dragonfly.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;

import java.io.InputStream;

public class AliOssUtil {
    private static final String ENDPOINT = System.getenv("OSS_ENDPOINT") != null ?
            System.getenv("OSS_ENDPOINT") : "https://oss-cn-beijing.aliyuncs.com";
    private static final String ACCESS_KEY_ID = System.getenv("OSS_ACCESS_KEY_ID");
    private static final String ACCESS_KEY_SECRET = System.getenv("OSS_ACCESS_KEY_SECRET");
    private static final String BUCKET_NAME = System.getenv("OSS_BUCKET_NAME") != null ?
            System.getenv("OSS_BUCKET_NAME") : "dragonfly-blog";

    /**
     * 方式1：使用主账号AccessKey直接上传（管理员/后端使用）
     */
    public static String uploadFile(String objectName, InputStream in) throws Exception {
        if (ACCESS_KEY_ID == null || ACCESS_KEY_SECRET == null) {
            throw new IllegalStateException("请设置环境变量 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET");
        }

        String region = System.getenv("OSS_REGION") != null ?
                System.getenv("OSS_REGION") : "cn-beijing";

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = null;

        try {
            ossClient = OSSClientBuilder.create()
                    .endpoint(ENDPOINT)
                    .credentialsProvider(new DefaultCredentialProvider(ACCESS_KEY_ID, ACCESS_KEY_SECRET))
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(region)
                    .build();

            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, in);
            ossClient.putObject(putObjectRequest);

            String endpoint = ENDPOINT.replaceFirst("https?://", "");
            return "https://" + BUCKET_NAME + "." + endpoint + "/" + objectName;

        } catch (OSSException oe) {
            throw new Exception("OSS上传失败 - Error Code: " + oe.getErrorCode()
                    + ", Error Message: " + oe.getErrorMessage()
                    + ", Request ID: " + oe.getRequestId());
        } catch (ClientException ce) {
            throw new Exception("OSS客户端异常 - " + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 方式2：使用STS临时凭证上传（普通用户使用）
     * @param objectName 对象名称
     * @param in 输入流
     * @param accessKeyId STS临时AccessKeyId
     * @param accessKeySecret STS临时AccessKeySecret
     * @param securityToken STS安全令牌
     */
    public static String uploadFileWithSts(String objectName, InputStream in,
                                           String accessKeyId, String accessKeySecret,
                                           String securityToken) throws Exception {
        String region = System.getenv("OSS_REGION") != null ?
                System.getenv("OSS_REGION") : "cn-beijing";

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        OSS ossClient = null;

        try {
            // ✅ 修正点：直接用 accessKeyId, accessKeySecret, securityToken 创建凭证
            ossClient = OSSClientBuilder.create()
                    .endpoint(ENDPOINT)
                    .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret, securityToken))
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(region)
                    .build();

            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, in);
            ossClient.putObject(putObjectRequest);

            String endpoint = ENDPOINT.replaceFirst("https?://", "");
            return "https://" + BUCKET_NAME + "." + endpoint + "/" + objectName;

        } catch (OSSException oe) {
            throw new Exception("OSS上传失败(STS) - Error Code: " + oe.getErrorCode()
                    + ", Error Message: " + oe.getErrorMessage()
                    + ", Request ID: " + oe.getRequestId());
        } catch (ClientException ce) {
            throw new Exception("OSS客户端异常(STS) - " + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}