# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Dragonfly Blog 是一个前后端分离的笔记/博客应用（"小红书"风格的笔记分享平台）。后端使用 Spring Boot 3，前端使用 Vue 3。后端代码位于仓库根目录（`src/main/java/com/dragonfly/`），前端代码位于 `frontend/`。

## 常用命令

### 后端（Maven，Java 17，Spring Boot 3.1.3）

```bash
mvn spring-boot:run          # 启动 API 服务，端口 :8080
mvn test                     # 运行所有测试
mvn -Dtest=ClassName test    # 运行单个测试类
mvn -Dtest=ClassName#method test   # 运行单个测试方法
mvn clean package            # 构建可执行 jar
```

测试很少（`src/test/java` 下只有初始的 `DragonflyBlogApplicationTest` 和 `Demo`）。未配置 lint 步骤。

### 前端（Vite + Vue 3）

```bash
cd frontend
npm install        # 安装依赖
npm run dev        # 开发服务器（Vite），将 /api 代理到 localhost:8080
npm run build      # 生产构建到 frontend/dist
npm run format     # 对 src/ 执行 prettier
```

要求 Node 版本 `^20.19.0 || >=22.12.0`。

### 基础设施（Docker）

```bash
docker compose up -d   # MySQL 8、Redis 7、Elasticsearch 8、Kibana、Zookeeper、Kafka
```

参见 `docker-compose.yml`。注意：它挂载了 `./init.sql` 用于 MySQL 首次启动建表，但该文件在仓库中**不存在**——数据库表结构需手动创建（仅 `src/main/resources/db/` 下有部分迁移 SQL）。

## 后端架构

分层 Spring MVC 模式：`controller` → `service`（`service/impl`）→ `mapper`（MyBatis-Plus）。`pojo` 存放实体类，`vo` 存放 DTO，`config` 存放 Spring 配置，`utils` 存放工具类。

**响应与分页约定**
- 每个 controller 都返回 `Result<T>`（`pojo/Result.java`）：`code=0` 成功，`code=1` 失败，带 `message` 和 `data`。使用静态方法 `Result.success(data)`、`Result.success()`、`Result.error(msg)`。
- 分页返回 `PageBean<T>`（`pojo/PageBean.java`，含 `total` + `items`）。分页在 mapper 中**手动 offset/limit**（传入 `offset = (pageNum-1)*pageSize` 和 `pageSize`），而非 PageHelper——尽管 PageHelper 是已声明的依赖。

**数据访问（MyBatis-Plus）**
- Mapper 接口（`com.dragonfly.mapper.*`）使用 `@Mapper` 注解，绝大多数查询使用**注解 SQL**（`@Select`/`@Insert`/`@Update`/`@Delete`）。动态 SQL 在注解内使用 `<script>` 内联。
- 只有 `NoteMapper` 有 XML 配套文件：`src/main/resources/com/dragonfly/mapper/NoteMapper.xml`（通过 `application.yml` 中的 `mybatis-plus.mapper-locations` 解析）。
- 已开启 `map-underscore-to-camel-case`，因此数据库列如 `create_user` 映射到 `createUser`。`Note.images` 是 JSON 列，使用 `com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler`（`List<String>`）。

**认证与授权**
- 使用 `com.auth0:java-jwt`（`utils/JwtUtil.java`，HMAC256 密钥 `"dragonfly"`）签发 JWT。Token 在登录时签发并存入 **Redis**；`LoginInterceptor` 校验 `Authorization` 头中的 token（与 Redis 比对），并将 JWT claims 放入 `ThreadLocalUtil`（通过 `ThreadLocalUtil.get()` 获取，是一个含 `id`、`role` 等字段的 `Map`）。
- `RoleInterceptor` + `@RequireRole` 注解（`anno/RequireRole.java`）保护 `/admin/**` 路径。角色不区分大小写：`admin`、`super_admin`、`user`。当 `allowSuperAdmin=true`（默认）时，`super_admin` 自动放行。
- 拦截器顺序很重要：`WebConfig.addInterceptors` 先注册 `LoginInterceptor`，再注册 `RoleInterceptor`。公共端点（login/register/upload）在 `WebConfig` 中被排除。

**支撑设施**
- Redis：`config/RedisConfig.java` 定义了 JSON 序列化的 `RedisTemplate<String,Object>`；`utils/RedisCache.java` 封装常用操作。点赞/收藏计数缓存在 Redis 中，由 `task/LikeCountSyncTask.java`（`@Scheduled` 每 5 分钟一次，另加每日凌晨全量同步）刷回 MySQL。
- 全局异常由 `exception/GlobalExceptionHandle.java`（`@RestControllerAdvice`）处理——返回 `Result.error(msg)`，不让堆栈信息泄露。
- 文件上传使用阿里云 OSS（`utils/AliOssUtil.java`），通过**环境变量**配置——参见 `OSS_ENV_CONFIG.md`（`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET` 等）。
- `application.yml` 连接 MySQL、Redis、Elasticsearch、Kafka 和 RabbitMQ。**已声明但尚未使用**：Elasticsearch、Kafka、RabbitMQ、WebSocket、LangChain4j 都在 `pom.xml` 中，但尚无运行时代码（`es/` 包为空；暂无 `@KafkaListener`/`KafkaTemplate`/WebSocket 用法）。

## 业务约定（中文领域取值）

领域代码中使用了中文字符串字面量——必须完全匹配：
- 笔记发布状态（`Note.state`）：`"已发布"`（已发布）vs. `"草稿"`（草稿）。逻辑基于此字符串分支，以维护话题计数和发布时间。
- 角色：`"admin"`、`"super_admin"`、`"user"`（比较时忽略大小写）。
- 笔记类型通过 `note_category`（图文/视频）区分；`Note` 同时携带 `images` 和 `video`/`cover_img`。

## 前端架构

Vue 3 + Vite + Element Plus + Pinia + vue-router + axios + ECharts。路径别名 `@` → `frontend/src`。

- `src/utils/request.js` 是 axios 实例。`baseURL = '/api'`；Vite 开发服务器将 `/api` 代理到 `http://localhost:8080` 并去掉 `/api` 前缀（`vite.config.js`），因此后端路由不带 `/api` 前缀，也没有 servlet context path。
- Token/角色处理支持多标签页：token 存在 Pinia store（`src/stores/token.js`、`src/stores/userInfo.js`）中，同时镜像到 `localStorage`（`global_token`、`global_userInfo`）和各标签页的 `sessionStorage`（`token_${tabId}`、`userInfo_${tabId}`）。请求拦截器和路由守卫都会依次回退到这些来源。
- `src/router/index.js` 通过 `meta.requiresAuth`/`meta.role` 按路由强制认证和角色校验，并在 `/user/*` 与 `/admin/*` 路径间重定向管理员。角色归一化辅助函数在 `src/utils/roles.js`。
- 布局：`src/layouts/UserLayout.vue`（主应用）和 `src/layouts/AdminLayout.vue`（管理后台）。管理页面位于 `src/views/admin/`，用户页面位于 `src/views/user/`。
- API 模块是 `src/api/` 下按领域划分的薄封装（如 `note.js`、`user.js`、`admin.js`）。
