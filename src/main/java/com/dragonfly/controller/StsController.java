
package com.dragonfly.controller;

import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.StsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * STS临时凭证控制器
 *
 * @author 蜻蜓大王
 * @date 2026/4/29
 */
@RestController
@RequestMapping("/sts")
public class StsController {

    /**
     * 获取STS临时凭证
     * @return 临时凭证信息
     */
    @GetMapping("/credential")
    public Result<Map<String, String>> getStsCredential() {
        try {
            AssumeRoleResponse response = StsService.getStsCredential();

            Map<String, String> credentials = new HashMap<>();
            credentials.put("accessKeyId", response.getCredentials().getAccessKeyId());
            credentials.put("accessKeySecret", response.getCredentials().getAccessKeySecret());
            credentials.put("securityToken", response.getCredentials().getSecurityToken());
            credentials.put("expiration", response.getCredentials().getExpiration());

            return Result.success(credentials);
        } catch (Exception e) {
            return Result.error("获取STS临时凭证失败：" + e.getMessage());
        }
    }
}
