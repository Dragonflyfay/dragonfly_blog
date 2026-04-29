package com.dragonfly.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import org.springframework.stereotype.Service;

/**
 * 阿里云STS服务类
 * 用于获取临时访问凭证
 *
 * @author 蜻蜓大王
 * @date 2026/4/29
 */
@Service
public class StsService {

    private static final String ACCESS_KEY_ID = System.getenv("OSS_ACCESS_KEY_ID OSS_ACCESS_KEY_ID");
    private static final String ACCESS_KEY_SECRET = System.getenv("OSS_ACCESS_KEY_SECRET");
    private static final String ROLE_ARN = System.getenv("STS_ROLE_ARN");
    private static final String REGION = System.getenv("OSS_REGION") != null ?
            System.getenv("OSS_REGION") : "cn-beijing";

    /**
     * 获取STS临时凭证
     * @return AssumeRoleResponse 包含临时AK、SK和Token
     * @throws ClientException 异常
     */
    public static AssumeRoleResponse getStsCredential() throws ClientException {
        if (ACCESS_KEY_ID == null || ACCESS_KEY_SECRET == null || ROLE_ARN == null) {
            throw new IllegalStateException("请设置环境变量 OSS_ACCESS_KEY_ID、OSS_ACCESS_KEY_SECRET 和 STS_ROLE_ARN");
        }

        IClientProfile profile = DefaultProfile.getProfile(REGION, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        IAcsClient client = new DefaultAcsClient(profile);

        AssumeRoleRequest request = new AssumeRoleRequest();
        request.setSysEndpoint("sts." + REGION + ".aliyuncs.com");
        request.setSysMethod(MethodType.POST);
        request.setRoleArn(ROLE_ARN);
        request.setRoleSessionName("BlogUploadSession");
        request.setDurationSeconds(3600L);

        try {
            AssumeRoleResponse response = client.getAcsResponse(request);
            return response;
        } catch (ClientException e) {
            System.out.println("获取STS临时凭证失败：" + e.getErrMsg());
            throw e;
        }
    }
}
