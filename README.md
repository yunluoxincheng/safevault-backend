# SafeVault 后端服务

## 项目概述

SafeVault 后端服务为 Android 密码管理器应用提供云端支持，实现用户管理、应用内密码分享、实时通信等核心功能。

### 技术栈

- **框架**: Spring Boot 3.5.x
- **Java 版本**: JDK 17+
- **数据库**: PostgreSQL
- **ORM**: Spring Data JPA + Flyway
- **安全**: Spring Security + JWT
- **加密**: Bouncy Castle (AES-256-GCM, EC)
- **实时通信**: WebSocket (STOMP)
- **API 文档**: SpringDoc OpenAPI
- **构建工具**: Maven
- **容器化**: Docker + Docker Compose

### 核心功能模块

```
safevault-backend/
├── 用户管理模块 (User Management)
│   ├── 用户注册/登录
│   ├── JWT 认证
│   ├── 密钥对管理
│   ├── 用户搜索
│   └── 用户配置文件管理
│
├── 密码分享模块 (Share Management)
│   ├── 三种分享方式
│   │   ├── 通过用户ID/用户名直接分享
│   │   ├── 扫码分享（类似 AirDrop）
│   │   └── 附近设备发现分享
│   ├── 创建/接收/撤销分享
│   ├── 分享历史管理
│   ├── 有效期控制
│   └── 权限控制
│
├── 实时通信模块 (WebSocket)
│   ├── 分享通知推送
│   ├── 在线状态管理
│   └── 连接心跳保活
│
├── 附近发现模块 (Discovery)
│   ├── 位置注册
│   ├── 附近用户查询
│   └── 距离计算（Haversine 公式）
│
└── 加密服务模块 (Crypto Service)
    ├── 用户密钥对生成
    ├── 会话密钥生成
    ├── 端到端加密
    └── 分享数据加解密
```

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+

### 本地运行

```bash
# 克隆项目
git clone https://github.com/your-org/safevault-backend.git
cd safevault-backend

# 启动数据库
docker-compose up -d postgres

# 运行应用
./mvnw spring-boot:run

# 访问 API 文档
open http://localhost:8080/api/swagger-ui.html
```

### Docker 部署

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 项目结构

```
safevault-backend/
├── pom.xml                                    # Maven 配置
├── Dockerfile                                 # Docker 镜像构建
├── docker-compose.yml                         # 本地开发环境
├── src/
│   ├── main/
│   │   ├── java/org/ttt/safevaultbackend/
│   │   │   ├── SafevaultBackendApplication.java    # 主启动类
│   │   │   │
│   │   │   ├── config/                            # 配置类
│   │   │   │   ├── SecurityConfig.java            # 安全配置
│   │   │   │   ├── WebSocketConfig.java          # WebSocket 配置
│   │   │   │   └── OpenApiConfig.java             # API 文档配置
│   │   │   │
│   │   │   ├── controller/                        # REST API 控制器
│   │   │   │   ├── AuthController.java            # 认证接口
│   │   │   │   ├── UserController.java            # 用户接口
│   │   │   │   ├── ShareController.java           # 分享接口
│   │   │   │   └── DiscoveryController.java       # 附近发现接口
│   │   │   │
│   │   │   ├── service/                           # 业务逻辑层
│   │   │   │   ├── AuthService.java               # 认证服务
│   │   │   │   ├── UserService.java               # 用户服务
│   │   │   │   ├── ShareService.java              # 分享服务
│   │   │   │   ├── WebSocketService.java          # WebSocket 服务
│   │   │   │   ├── DiscoveryService.java          # 附近发现服务
│   │   │   │   └── CryptoService.java             # 加密服务
│   │   │   │
│   │   │   ├── repository/                        # 数据访问层
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── PasswordShareRepository.java
│   │   │   │   ├── ShareAuditLogRepository.java
│   │   │   │   └── OnlineUserRepository.java
│   │   │   │
│   │   │   ├── entity/                            # 数据库实体
│   │   │   │   ├── User.java
│   │   │   │   ├── PasswordShare.java
│   │   │   │   ├── ShareAuditLog.java
│   │   │   │   ├── OnlineUser.java
│   │   │   │   ├── ShareStatus.java               # 分享状态枚举
│   │   │   │   └── ShareType.java                 # 分享类型枚举
│   │   │   │
│   │   │   ├── dto/                               # 数据传输对象
│   │   │   │   ├── request/                       # 请求 DTO
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── CreateShareRequest.java
│   │   │   │   │   └── RegisterLocationRequest.java
│   │   │   │   ├── response/                      # 响应 DTO
│   │   │   │   │   ├── AuthResponse.java
│   │   │   │   │   ├── UserProfileResponse.java
│   │   │   │   │   ├── ShareResponse.java
│   │   │   │   │   └── NearbyUserResponse.java
│   │   │   │   ├── SharePermission.java
│   │   │   │   ├── PasswordData.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── ShareNotificationMessage.java
│   │   │   │   └── SharePackage.java
│   │   │   │
│   │   │   ├── security/                          # 安全相关
│   │   │   │   ├── JwtTokenProvider.java          # JWT 工具
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   │
│   │   │   ├── exception/                         # 异常处理
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   └── BusinessException.java
│   │   │   │
│   │   │   ├── websocket/                         # WebSocket 相关
│   │   │   │   ├── WebSocketAuthInterceptor.java
│   │   │   │   ├── WebSocketConnectionManager.java
│   │   │   │   └── WebSocketEventListener.java
│   │   │   │
│   │   │   ├── scheduler/                         # 定时任务
│   │   │   │   ├── ShareExpirationScheduler.java
│   │   │   │   └── OnlineUserCleanupScheduler.java
│   │   │   │
│   │   │   └── util/                              # 工具类
│   │   │       └── KeyGenerator.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                    # 主配置文件
│   │       ├── application-dev.yml                # 开发环境配置
│   │       ├── application-prod.yml               # 生产环境配置
│   │       └── db/migration/                      # 数据库迁移脚本
│   │
│   └── test/
│       └── java/org/ttt/safevaultbackend/
│           └── SafevaultBackendApplicationTests.java
```

## API 接口文档

### 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token
- **响应格式**: JSON
- **API 文档**: `http://localhost:8080/api/swagger-ui.html`

### 1. 认证接口

#### 1.1 用户注册

```http
POST /v1/auth/register
Content-Type: application/json

{
  "deviceId": "unique-device-id",
  "username": "zhangsan",
  "displayName": "张三",
  "publicKey": "BASE64_ENCODED_PUBLIC_KEY"
}

Response 201:
{
  "userId": "usr_abc123xyz",
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}
```

#### 1.2 用户登录

```http
POST /v1/auth/login
Content-Type: application/json

{
  "userId": "usr_abc123xyz",
  "deviceId": "unique-device-id",
  "signature": "USER_SIGNATURE"
}

Response 200:
{
  "userId": "usr_abc123xyz",
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}
```

### 2. 用户管理接口

#### 2.1 获取当前用户信息

```http
GET /v1/users/me
Authorization: Bearer {access_token}
```

#### 2.2 搜索用户

```http
GET /v1/users/search?query=zhang
Authorization: Bearer {access_token}

Response 200:
[
  {
    "userId": "usr_def456uvw",
    "username": "zhangsan",
    "displayName": "张三",
    "publicKey": "BASE64_ENCODED_PUBLIC_KEY"
  }
]
```

#### 2.3 生成二维码（用于扫码分享）

```http
GET /v1/users/me/qrcode
Authorization: Bearer {access_token}

Response 200:
{
  "qrCodeData": "safevault:receive:usr_abc123xyz:temp_token",
  "expiresAt": 1704672000000,
  "size": 300
}
```

### 3. 密码分享接口

#### 3.1 创建分享（支持三种类型）

```http
POST /v1/shares
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "passwordId": "local_pwd_123",
  "title": "GitHub",
  "username": "user@example.com",
  "encryptedPassword": "ENCRYPTED_PASSWORD",
  "url": "https://github.com",
  "notes": "我的GitHub账号",
  "toUserId": "usr_def456uvw",
  "expireInMinutes": 1440,
  "permission": {
    "canView": true,
    "canSave": true,
    "isRevocable": true
  },
  "shareType": "USER_TO_USER"
}

Response 200:
{
  "shareId": "shr_abc123xyz",
  "shareToken": null,
  "expiresAt": 1704240000000
}
```

**分享类型（shareType）**：
- `DIRECT` - 直接分享（链接/二维码，无需指定接收方）
- `USER_TO_USER` - 用户对用户分享（通过用户ID/用户名）
- `NEARBY` - 附近设备分享

#### 3.2 接收分享

```http
GET /v1/shares/{shareId}
Authorization: Bearer {access_token}

Response 200:
{
  "shareId": "shr_abc123xyz",
  "fromUserId": "usr_abc123xyz",
  "fromDisplayName": "张三",
  "passwordData": {
    "title": "GitHub",
    "username": "user@example.com",
    "encryptedPassword": "ENCRYPTED_PASSWORD",
    "url": "https://github.com",
    "notes": "我的GitHub账号"
  },
  "permission": {
    "canView": true,
    "canSave": true,
    "isRevocable": true
  },
  "status": "PENDING",
  "shareType": "USER_TO_USER",
  "createdAt": 1704067200000,
  "expiresAt": 1704240000000
}
```

#### 3.3 撤销分享

```http
POST /v1/shares/{shareId}/revoke
Authorization: Bearer {access_token}
```

#### 3.4 保存分享的密码

```http
POST /v1/shares/{shareId}/save
Authorization: Bearer {access_token}
```

#### 3.5 获取创建的分享

```http
GET /v1/shares/created
Authorization: Bearer {access_token}
```

#### 3.6 获取接收的分享

```http
GET /v1/shares/received
Authorization: Bearer {access_token}
```

### 4. 附近发现接口

#### 4.1 注册位置信息

```http
POST /v1/discovery/register
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "latitude": 39.9042,
  "longitude": 116.4074
}
```

#### 4.2 获取附近用户

```http
GET /v1/discovery/nearby?lat=39.9042&lng=116.4074&radius=1000
Authorization: Bearer {access_token}

Response 200:
[
  {
    "userId": "usr_def456uvw",
    "username": "lisi",
    "displayName": "李四",
    "distance": 256.5,
    "lastSeen": 1704067200000
  }
]
```

#### 4.3 发送心跳

```http
POST /v1/discovery/heartbeat
Authorization: Bearer {access_token}
```

### 5. WebSocket 接口

#### 5.1 连接端点

```
ws://localhost:8080/api/ws
```

#### 5.2 订阅主题

- `/user/queue/shares` - 接收分享通知（用户专属）
- `/topic/online-users` - 在线用户列表（附近发现功能）

#### 5.3 消息格式

**分享通知消息**：
```json
{
  "type": "NEW_SHARE",
  "shareId": "shr_abc123",
  "fromUserId": "usr_abc123xyz",
  "fromDisplayName": "张三",
  "message": "张三向你分享了一个密码",
  "timestamp": 1704067200000
}
```

**在线用户消息**：
```json
{
  "userId": "usr_def456uvw",
  "username": "lisi",
  "displayName": "李四",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "lastSeen": 1704067200000
}
```

## 三种分享方式

### 方式 1：通过用户ID/用户名直接分享

1. 分享方搜索用户（输入ID或用户名）
2. 后端返回匹配的用户列表
3. 分享方选择接收方和要分享的密码
4. 设置权限和有效期
5. 后端创建分享记录
6. 后端通过 WebSocket 推送通知给接收方
7. 接收方 App 显示分享通知

### 方式 2：扫码分享（类似 AirDrop）

1. 接收方打开 App，显示"接收分享"二维码（包含用户ID和临时Token）
2. 分享方点击"扫码分享"，扫描接收方二维码
3. 分享方选择要分享的密码
4. 设置权限和有效期
5. 后端创建分享记录
6. 后端通过 WebSocket 推送通知给接收方
7. 接收方 App 立即显示分享内容

### 方式 3：附近设备发现（类似蓝牙分享）

1. 双方打开 App 的"附近分享"功能
2. App 通过 WebSocket 注册位置信息
3. 后端返回附近的 SafeVault 用户列表
4. 分享方选择附近用户
5. 分享方选择密码并分享
6. 后端通过 WebSocket 推送给接收方

## 数据库设计

### 主要表结构

- **users** - 用户表
  - 用户ID、设备ID、用户名、显示名称、公钥
  - 创建的分享、接收的分享

- **password_shares** - 密码分享表
  - 分享ID、密码ID、分享方、接收方、加密数据
  - 创建时间、过期时间、权限、状态、分享类型

- **share_audit_logs** - 分享审计日志表
  - 分享操作记录（创建、接收、撤销等）

- **online_users** - 在线用户表
  - 用户ID、用户名、显示名称、位置信息
  - 最后活跃时间、会话ID

## 安全设计

### 1. 认证与授权
- JWT 认证机制
- Token 有效期控制（24小时）
- Refresh Token 轮换
- API 权限控制
- WebSocket 连接认证

### 2. 数据加密
- 用户密钥对：EC P-521
- 会话密钥：AES-256-GCM
- 传输加密：HTTPS + WSS (WebSocket Secure)
- 签名验证：HMAC-SHA256

### 3. 数据保护
- 密码数据不在服务端明文存储
- 会话密钥使用后立即销毁
- 敏感数据不记录日志
- 数据库连接加密

## 错误码定义

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| `AUTH_INVALID_TOKEN` | 无效的访问令牌 | 401 |
| `AUTH_TOKEN_EXPIRED` | 令牌已过期 | 401 |
| `USER_NOT_FOUND` | 用户不存在 | 404 |
| `USER_ALREADY_EXISTS` | 用户已存在 | 409 |
| `SHARE_NOT_FOUND` | 分享不存在 | 404 |
| `SHARE_EXPIRED` | 分享已过期 | 403 |
| `SHARE_REVOKED` | 分享已撤销 | 410 |
| `SAVE_NOT_ALLOWED` | 不允许保存 | 403 |
| `INVALID_ENCRYPTED_DATA` | 加密数据无效 | 400 |
| `ACCESS_DENIED` | 无权访问 | 403 |

## 定时任务

### 分享过期检查
- **频率**：每小时一次
- **功能**：将过期的分享状态更新为 EXPIRED

### 在线用户清理
- **频率**：每分钟一次
- **功能**：清理 2 分钟未活跃的在线用户记录

## 开发指南

### API 测试

```bash
# 注册用户
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "test-device-001",
    "username": "testuser",
    "displayName": "测试用户",
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
  }'

# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "usr_abc123xyz",
    "deviceId": "test-device-001",
    "signature": "signature"
  }'

# 搜索用户
curl -X GET "http://localhost:8080/api/v1/users/search?query=test" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 创建分享
curl -X POST http://localhost:8080/api/v1/shares \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "passwordId": "pwd_123",
    "title": "测试密码",
    "username": "test@example.com",
    "encryptedPassword": "encrypted_password_here",
    "toUserId": "usr_def456uvw",
    "expireInMinutes": 1440,
    "permission": {
      "canView": true,
      "canSave": true,
      "isRevocable": true
    },
    "shareType": "USER_TO_USER"
  }'
```

## 许可证

Copyright © 2024 SafeVault. All rights reserved.
