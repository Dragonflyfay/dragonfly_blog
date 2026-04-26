package com.dragonfly.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;

import java.io.InputStream;

/**
 * 阿里云OSS工具类
 * 敏感配置从环境变量中读取，避免硬编码在代码中
 *
 * @author 蜻蜓大王
 * @date 2026/4/24 21:49
 */
public class AliOssUtil {
    // 从环境变量中读取配置，避免密钥硬编码在代码中
    private static final String ENDPOINT = System.getenv("OSS_ENDPOINT") != null ? 
        System.getenv("OSS_ENDPOINT") : "https://oss-cn-beijing.aliyuncs.com";
    private static final String ACCESS_KEY_ID = System.getenv("OSS_ACCESS_KEY_ID");
    private static final String ACCESS_KEY_SECRET = System.getenv("OSS_ACCESS_KEY_SECRET");
    private static final String BUCKET_NAME = System.getenv("OSS_BUCKET_NAME") != null ? 
        System.getenv("OSS_BUCKET_NAME") : "dragonfly-blog";
    
    public static String uploadFile(String objectName, InputStream in) throws Exception {
        // 检查必要的环境变量是否已设置
        if (ACCESS_KEY_ID == null || ACCESS_KEY_SECRET == null) {
            throw new IllegalStateException("请设置环境变量 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET");
        }
        
        // 从环境变量获取Region，默认为cn-beijing
        String region = System.getenv("OSS_REGION") != null ? 
            System.getenv("OSS_REGION") : "cn-beijing";

        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(ENDPOINT)
                .credentialsProvider(new DefaultCredentialProvider(ACCESS_KEY_ID, ACCESS_KEY_SECRET))
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        String url = "";
        try {
            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, in);

            // 上传文件。
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            //url组成 https://bucket名称.区域节点/objectName
            url = "https://" + BUCKET_NAME + "." + ENDPOINT.replaceFirst("https?://", "") + "/" + objectName;
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }
}
