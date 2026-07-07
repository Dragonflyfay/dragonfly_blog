# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**语言要求：后续所有回复请使用中文。**

## Project Overview

Dragonfly Blog — a full-stack blog/social media application. Java 17 + Spring Boot 3.1.3 backend (Maven), Vue 3 + Vite + Element Plus frontend (in `frontend/`). MySQL via MyBatis-Plus, Redis 7, Elasticsearch 8.11, Alibaba Cloud OSS for file storage.

## Build & Run Commands

### Infrastructure (required before running the app)

```bash
docker compose up -d    # Start ES, Kibana, Redis
```

### Backend (from project root)

```bash
mvn spring-boot:run                                    # Run (needs OSS env vars set, see below)
mvn spring-boot:run -Dspring-boot.run.environmentVariables.OSS_ACCESS_KEY_ID=<id> -Dspring-boot.run.environmentVariables.OSS_ACCESS_KEY_SECRET=<secret>
mvn test                                               # Run all tests
mvn test -Dtest=ClassName                              # Run a single test class
mvn clean package                                      # Build JAR
```

The app starts on `localhost:8080` with MySQL on `localhost:3306` (db: `dragonfly_blog`, user: `root`, pw: `123456`).

### Frontend (from `frontend/`)

```bash
npm install     # First time only
npm run dev     # Dev server with hot reload (Vite proxys /api → localhost:8080)
npm run build   # Production build
```

### Required Environment Variables

The OSS file upload component reads credentials from the environment at startup. Without these, `FileUploadController` will fail:

- `OSS_ACCESS_KEY_ID` (required)
- `OSS_ACCESS_KEY_SECRET` (required)
- `OSS_ENDPOINT` (default: `https://oss-cn-beijing.aliyuncs.com`)
- `OSS_BUCKET_NAME` (default: `dragonfly-blog`)
- `OSS_REGION` (default: `cn-beijing`)

See `OSS_ENV_CONFIG.md` for setup instructions per shell/IDE.

## Architecture

```
Vue 3 SPA (port 5173/Vite dev)  ──/api──>  Spring Boot (port 8080)
                                               │
                          ┌────────────────────┼────────────────────┐
                          │                    │                    │
                        MySQL           Redis (token)       Alibaba OSS
                   (dragonfly_blog)   Elasticsearch (WIP)
```

### Backend Layers

**Interceptor chain** (in order): `LoginInterceptor` → `RoleInterceptor`

- `LoginInterceptor`: Extracts JWT from `Authorization` header, validates against Redis (12h TTL), stores claims in `ThreadLocalUtil`. Excludes `/user/login`, `/user/register`, `/upload`.
- `RoleInterceptor`: Only on `/admin/**` paths. Reads `@RequireRole` from handler methods, checks role from ThreadLocal. Returns 403 on mismatch.

**Standard three-tier flow**: Controller → Service → Mapper. Controllers return `Result<T>` (code=0 success, code=1 error). Exceptions are caught by `GlobalExceptionHandle` (`@RestControllerAdvice`).

**Key packages**:
| Package | Purpose |
|---|---|
| `com.dragonfly.controller` | REST controllers |
| `com.dragonfly.service/impl` | Business logic |
| `com.dragonfly.mapper` | MyBatis-Plus mappers (only `NoteMapper` has XML) |
| `com.dragonfly.pojo` | Entities + `Result<T>` + `PageBean<T>` |
| `com.dragonfly.config` | `WebConfig` (interceptor registration) |
| `com.dragonfly.interceptors` | `LoginInterceptor`, `RoleInterceptor` |
| `com.dragonfly.anno` | `@State` (note state validation), `@RequireRole` |
| `com.dragonfly.utils` | `JwtUtil`, `Md5Utils`, `ThreadLocalUtil`, `AliOssUtil` |
| `com.dragonfly.es` | Elasticsearch code (package exists, empty — ES search not yet implemented) |

### Frontend Structure

Two layout/role zones, both require auth except `/login`:

- **`/admin/*`** (AdminLayout) → admin-only pages: dashboard, topic/note/comment/user management
- **`/*`** (UserLayout) → regular user pages: discover feed (waterfall), publish, notifications, profile
- Route guard in `router/index.js` checks JWT token and role, redirects admin users from `/user/*` to `/admin/user/*`

State: Pinia stores (token, userInfo) with `pinia-plugin-persistedstate`. API calls via Axios modules in `src/api/`.

### Auth Flow

1. Register or login → backend MD5-hashes password, issues JWT (claims: id, username, role)
2. JWT stored in Redis (key=token value, 12h TTL) and sent to client
3. Client includes `Authorization: <token>` on subsequent requests
4. `LoginInterceptor` validates token against Redis on each request
5. `ThreadLocalUtil` makes user claims available to controllers/services

### Note States

Notes have a `state` field validated by `@State`. Published notes appear in user-facing queries; drafts are excluded. The `Note` entity stores images as a JSON array (`JacksonTypeHandler`), supports image/video categories, and tracks view/like/comment/favorite counts.

### Social Features

Likes, favorites, follows, comments (multi-level nested tree via `parentId` + `children` list on `Comment`), and view tracking — each with a dedicated controller/service/mapper.

## Database

MySQL `dragonfly_blog` database. MyBatis-Plus handles most CRUD; `NoteMapper.xml` is the only XML mapper. Only one DDL file exists in resources (`db/create_favorite_record_table.sql`). Other tables are created via MyBatis-Plus auto-DDL or external migration. Config maps `underscore_to_camel_case` for both MyBatis and MyBatis-Plus.

## Docker Compose Services

- **elasticsearch** (8.11.0): port 9200/9300, single-node, security disabled, 512MB heap
- **kibana** (8.11.0): port 5601, connected to ES
- **redis** (7.2-alpine): port 6379, persistent volume
- All on `dragonfly-network` bridge network

No Dockerfile exists for the application itself — only infrastructure is containerized.

## Git & Branching

- Default branch: `master`
- Author: DragonflyFay
- Recent work focused on social features (follow, favorite), search optimization, and Docker setup
