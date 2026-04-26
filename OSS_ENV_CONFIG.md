# 阿里云OSS环境变量配置说明

## 概述
为了保护敏感的访问密钥，项目已改为从环境变量中读取OSS配置信息。

## 需要设置的环境变量

| 环境变量名 | 说明 | 示例值 | 是否必需 |
|-----------|------|--------|---------|
| OSS_ACCESS_KEY_ID | 阿里云AccessKey ID | LTAI5t9fqrYtFiCxVgRhdcDK | 是 |
| OSS_ACCESS_KEY_SECRET | 阿里云AccessKey Secret | yJSQUpfPlmhPsuXv9RBVq6zjLuLPCQ | 是 |
| OSS_ENDPOINT | OSS访问端点 | https://oss-cn-beijing.aliyuncs.com | 否（有默认值） |
| OSS_BUCKET_NAME | Bucket名称 | dragonfly-blog | 否（有默认值） |
| OSS_REGION | OSS区域 | cn-beijing | 否（有默认值） |

## Windows PowerShell 设置方法

### 临时设置（仅当前会话有效）
```powershell
$env:OSS_ACCESS_KEY_ID="your-access-key-id"
$env:OSS_ACCESS_KEY_SECRET="your-access-key-secret"
$env:OSS_ENDPOINT="https://oss-cn-beijing.aliyuncs.com"
$env:OSS_BUCKET_NAME="dragonfly-blog"
$env:OSS_REGION="cn-beijing"
```

### 永久设置（用户级别）
```powershell
[Environment]::SetEnvironmentVariable("OSS_ACCESS_KEY_ID", "your-access-key-id", "User")
[Environment]::SetEnvironmentVariable("OSS_ACCESS_KEY_SECRET", "your-access-key-secret", "User")
[Environment]::SetEnvironmentVariable("OSS_ENDPOINT", "https://oss-cn-beijing.aliyuncs.com", "User")
[Environment]::SetEnvironmentVariable("OSS_BUCKET_NAME", "dragonfly-blog", "User")
[Environment]::SetEnvironmentVariable("OSS_REGION", "cn-beijing", "User")
```

### 永久设置（系统级别，需要管理员权限）
```powershell
[Environment]::SetEnvironmentVariable("OSS_ACCESS_KEY_ID", "your-access-key-id", "Machine")
[Environment]::SetEnvironmentVariable("OSS_ACCESS_KEY_SECRET", "your-access-key-secret", "Machine")
[Environment]::SetEnvironmentVariable("OSS_ENDPOINT", "https://oss-cn-beijing.aliyuncs.com", "Machine")
[Environment]::SetEnvironmentVariable("OSS_BUCKET_NAME", "dragonfly-blog", "Machine")
[Environment]::SetEnvironmentVariable("OSS_REGION", "cn-beijing", "Machine")
```

## Windows CMD 设置方法

### 临时设置
```cmd
set OSS_ACCESS_KEY_ID=your-access-key-id
set OSS_ACCESS_KEY_SECRET=your-access-key-secret
set OSS_ENDPOINT=https://oss-cn-beijing.aliyuncs.com
set OSS_BUCKET_NAME=dragonfly-blog
set OSS_REGION=cn-beijing
```

### 永久设置
```cmd
setx OSS_ACCESS_KEY_ID "your-access-key-id"
setx OSS_ACCESS_KEY_SECRET "your-access-key-secret"
setx OSS_ENDPOINT "https://oss-cn-beijing.aliyuncs.com"
setx OSS_BUCKET_NAME "dragonfly-blog"
setx OSS_REGION "cn-beijing"
```

## IDEA 中设置环境变量

1. 打开 Run/Debug Configurations
2. 找到你的启动配置
3. 在 "Environment variables" 字段中添加：
   ```
   OSS_ACCESS_KEY_ID=your-access-key-id;OSS_ACCESS_KEY_SECRET=your-access-key-secret;OSS_ENDPOINT=https://oss-cn-beijing.aliyuncs.com;OSS_BUCKET_NAME=dragonfly-blog;OSS_REGION=cn-beijing
   ```

## Maven 运行时设置

```bash
mvn spring-boot:run -Dspring-boot.run.environmentVariables.OSS_ACCESS_KEY_ID=your-access-key-id
```

## 验证配置

运行以下Java代码验证环境变量是否正确设置：

```java
public class TestEnv {
    public static void main(String[] args) {
        System.out.println("OSS_ACCESS_KEY_ID: " + System.getenv("OSS_ACCESS_KEY_ID"));
        System.out.println("OSS_ACCESS_KEY_SECRET: " + System.getenv("OSS_ACCESS_KEY_SECRET"));
        System.out.println("OSS_ENDPOINT: " + System.getenv("OSS_ENDPOINT"));
        System.out.println("OSS_BUCKET_NAME: " + System.getenv("OSS_BUCKET_NAME"));
        System.out.println("OSS_REGION: " + System.getenv("OSS_REGION"));
    }
}
```

## 安全建议

1. **不要将密钥提交到版本控制系统**
   - 确保 `.gitignore` 文件中包含可能包含密钥的配置文件
   
2. **定期轮换密钥**
   - 建议每3-6个月更换一次AccessKey

3. **使用RAM用户**
   - 不要使用主账号的AccessKey
   - 为应用创建专用的RAM用户，并授予最小权限

4. **不同环境使用不同密钥**
   - 开发、测试、生产环境应使用不同的AccessKey

## 故障排除

如果运行时出现 "请设置环境变量 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET" 错误：

1. 确认环境变量已正确设置
2. 重启IDE或终端使环境变量生效
3. 检查环境变量名称是否完全匹配（区分大小写）
4. 使用上述验证代码测试环境变量是否可读取
