# 校园演出订票系统后端接口文档

本项目为校园演出订票系统后端，采用 Spring Boot + JPA 实现，支持多角色认证、权限控制、资料管理以及智能头像处理等功能。本文档着重介绍主要接口使用方法及其实现设计，便于后续理解和说明系统结构。

---

## 一、系统结构与分层设计

- 接口层（Controller）：负责接收HTTP请求、参数解析、权限校验、响应结果，仅做请求分发。
- 业务逻辑层（Service）：负责所有业务判断、权限检查、数据聚合、头像处理等具体操作。
- 数据访问层（Repository/DAO）：通过JPA实现数据库相关的操作。
- DTO层：定义对外暴露和分级返回的数据结构，保证敏感信息有针对性地隔离。
- 工具类层（Util）：如头像URL处理、文件相关辅助方法。

---

## 二、接口使用说明

### 1. 用户认证与注册

#### 登录 / 注册

- **请求路径**: `POST /api/auth/login`
- **说明**: 支持OpenID，首次登录自动注册，头像字段支持网络图片自动下载保存本地。

**请求参数（JSON）**
```json
{
  "openid": "openid_001",
  "nickname": "Alice",
  "avatar": "https://avatars.githubusercontent.com/u/1?v=4"
}
```

**返回参数**
```json
{
  "success": true,
  "data": {
    "token": "jwt-token",
    "userId": 1001,
    "openid": "openid_001",
    "nickname": "Alice",
    "avatar": "http://localhost:8080/data/avatar/xxxx.jpg",
    "role": "USER",
    "state": 1
  }
}
```

---

### 2. 用户信息相关接口

#### 获取当前登录用户

- **路径**: `GET /api/users/me`
- **用途**: 获取登录后个人全部信息

#### 获取指定用户信息

- **路径**: `GET /api/users/member?openId=openid_001`
- **说明**: 返回内容依据访问者角色不同。
    - 管理员账户返回全部字段
    - 普通用户仅获得公开字段（nickname、avatar、college、major）

**公开信息示例**
```json
{
  "success": true,
  "data": {
    "nickname": "Alice",
    "avatar": "...",
    "college": "信息学院",
    "major": "软件工程"
  }
}
```
**完整信息示例**（管理员账号）
```json
{
  "success": true,
  "data": {
    "userId": 1001,
    "openid": "openid_001",
    "nickname": "Alice",
    "avatar": "...",
    "college": "信息学院",
    "major": "软件工程",
    "studentNo": "20190001",
    "phone": "139xxxx0001",
    "userIdentity": 1,
    "role": "USER",
    "status": 1,
    "createTime": "...",
    "updateTime": "...",
    "lastLoginTime": "..."
  }
}
```

---

### 3. 用户资料更新

#### 个人资料修改

- **路径**: `PUT /api/users/profile`
- **参数**: 支持多字段修改（nickname、avatarFile、avatarUrl、userIdentity、studentNo、major、college、phone 等）
    - 支持头像网络URL或本地文件上传，自动保存并统一格式化

**上传图片请求示例（multipart/form-data）**
```http
PUT /api/users/profile
Content-Type: multipart/form-data

nickname: Bob
avatarFile: (图片二进制)
major: 软件工程
...
```

**通过链接更新头像示例**（form-urlencoded）
```http
PUT /api/users/profile
Content-Type: application/x-www-form-urlencoded

avatarUrl=https://example.com/pic.jpg
```

---

### 4. 管理员相关接口

#### 用户封禁 / 解封

- **路径**: `PUT /api/admin/users/ban?openId=openid_001&ban=true`
- **权限**: 仅管理员可用

#### 查询用户列表

- **路径**: `GET /api/admin/users/list`
- **权限**: 仅管理员可用
- **返回**: 所有用户详细数据

#### 更改用户角色

- **路径**: `PUT /api/admin/users/role?openId=openid_002&newRole=ADMIN`
- **权限**: 仅SUPER_ADMIN可用
- **角色限定**: `USER`、`VENUE_ADMIN`、`ADMIN`、`SUPER_ADMIN`（必须严格大小写一致），系统自动校验

---

## 三、头像处理机制

- 如果请求为网络图片，系统会下载下来并缩放后保存为本地文件，保证资源可用与统一管理
- 数据返回时，头像字段自动拼接为完整可访问URL，前端无需单独处理

---

## 四、接口错误响应格式

所有接口失败返回：
```json
{
  "success": false,
  "message": "错误描述"
}
```
便于前端统一处理与故障追踪。

---

## 五、设计要点补充

- 业务逻辑全部在 Service 层实现，保证 Controller 仅做简单参数收发与鉴权
- 按角色将返回数据分级（公开/全部信息分离），提升数据安全
- 参数和权限校验均采用后端统一控制
- 支持灵活头像处理、角色管理、封禁功能，便于后续系统扩展

---

本接口文档结合了接口规范与实现细节，以便开发、测试、后续说明或技术/系统分析时查阅。如有需要更多字段或业务逻辑说明，请查阅源码或负责人注释。