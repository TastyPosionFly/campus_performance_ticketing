# 校园活动售票系统接口文档

本文档包含系统主要接口说明，包括用户登录/注册、用户资料管理和管理员用户管理接口。

---

## 1. AuthController 接口说明（登录 / 注册）

### 基础信息
- Controller 包路径：`org.example.campus_performance_ticketing.api`
- 请求前缀：`/api/auth`
- 认证方式：登录成功后返回 JWT Token
- 返回格式：统一使用 `ApiResponse<T>`

### 配置说明

```properties
user.avatar.upload-dir=./data/avatar
user.avatar.base-url=http://localhost:8080
```

### 登录 / 注册接口
- 接口地址：`POST /api/auth/login`
- 请求参数（JSON）：

```json
{
  "openid": "string",
  "nickname": "string",
  "avatar": "https://avatar-url"
}
```

### 头像处理逻辑
- 后端下载第三方头像
- 使用 Thumbnailator 缩放至 200×200
- UUID 命名
- 数据库存储相对路径

### 返回示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "userId": 1,
    "avatar": "http://localhost:8080/data/avatar/xxx.jpg"
  }
}
```

### 依赖

```xml
<dependency>
  <groupId>net.coobird</groupId>
  <artifactId>thumbnailator</artifactId>
  <version>0.4.20</version>
</dependency>
```

### 目录结构

```
data/avatar/
```

---

## 2. UserController 接口说明（用户资料管理）

### 基础信息
- Controller 包路径：`org.example.campus_performance_ticketing.api`
- 请求前缀：`/api/users`
- 认证方式：JWT Token
- 返回格式：统一使用 `ApiResponse<T>`

### 接口列表
1. 获取当前登录用户信息
2. 更新个人资料

### 1. 获取当前登录用户信息
- 接口地址：`GET /api/users/me`
- 请求头：`Authorization: Bearer <JWT_TOKEN>`
- 返回示例：
```json
{
  "code":0,
  "message":"success",
  "data":{
    "id":1,
    "nickname":"Alice",
    "avatar":"http://localhost:8080/data/avatar/xxx.jpg",
    "studentNo":"20230101",
    "major":"计算机科学",
    "college":"上海大学",
    "phone":"13800000001"
  }
}
```

### 2. 获取指定用户信息
- 接口地址：`GET /api/users/me`
- 请求头：`Authorization: Bearer <JWT_TOKEN>`
- - 请求参数（multipart/form-data）：

| 参数名    | 必填 | 说明       |
|--------|------|----------|
| openId | 否 | 指定用户微信Id |

- 返回示例：
```json
{
  "code":0,
  "message":"success",
  "data":{
    "id":1,
    "nickname":"Alice",
    "avatar":"http://localhost:8080/data/avatar/xxx.jpg",
    "studentNo":"20230101",
    "major":"计算机科学",
    "college":"上海大学",
    "phone":"13800000001"
  }
}
```

### 3. 更新个人资料
- 接口地址：`PUT /api/users/profile`
- 请求头：`Authorization: Bearer <JWT_TOKEN>`
- 请求参数（multipart/form-data）：

| 参数名 | 必填 | 说明         |
|--------|------|------------|
| nickname | 否 | 昵称         |
| avatarFile | 否 | 上传头像文件     |
| avatarUrl | 否 | 使用已有头像 URL |
| userIdentity | 否 | 用户类型  1-学生 2-学校职工 3-校外人员      |
| studentNo | 否 | 学号         |
| major | 否 | 专业         |
| college | 否 | 学院         |
| phone | 否 | 手机号        |

- 头像处理逻辑：
  - 上传文件后缩放至 200×200
  - UUID 命名，存储相对路径 `/data/avatar/`
  - 外部 URL 可直接使用

- 返回示例：
```json
{
  "code":0,
  "message":"success",
  "data":{
    "id":1,
    "nickname":"Alice",
    "avatar":"http://localhost:8080/data/avatar/xxx.jpg",
    "studentNo":"20230101",
    "major":"计算机科学",
    "college":"上海大学",
    "phone":"13800000001"
  }
}
```

---

## 3. AdminUserController 接口说明（管理员封禁 / 解封）

### 基础信息
- Controller 包路径：`org.example.campus_performance_ticketing.api`
- 请求前缀：`/api/admin/users`
- 权限要求：管理员（Admin）
- 认证方式：JWT Token
- 返回格式：统一使用 `ApiResponse<T>`

### 封禁 / 解封用户接口
- 接口地址：`PUT /api/admin/users/ban`
- 请求头：`Authorization: Bearer <ADMIN_TOKEN>`
- 请求参数（Query）：

| 参数名    | 类型      | 必填 | 说明 |
|--------|---------|------|------|
| openId | String  | 是 | 目标用户 ID |
| ban    | boolean | 是 | true=封禁, false=解封 |

- 示例请求：
  PUT /api/admin/users/ban?userId=5&ban=true

### 业务逻辑说明
1. Controller 从 Authorization 头解析 JWT Token
2. 自动去除 Bearer 前缀
3. 调用 UserService.banOrUnbanUser(adminToken, userId, ban)
4. 业务层校验：是否为管理员、用户是否存在、当前状态是否允许变更

### 返回示例
- 封禁成功：
```json
{"code":0,"message":"success","data":{"id":5,"nickname":"Alice","status":0}}
```
- 解封成功：
```json
{"code":0,"message":"success","data":{"id":5,"nickname":"Alice","status":1}}
```
> status 说明：1=正常, 0=已封禁

### 异常说明
- 非管理员操作：返回无权限错误
- 用户不存在：返回业务失败信息
- Token 无效或过期：认证失败

### 设计说明
- 管理员接口统一放在 /api/admin/** 下
- 封禁逻辑集中在 UserService，Controller 简洁
- 使用 PUT 表示修改用户状态

### 适用场景
- 后台管理系统
- 校园活动 / 售票系统用户风控
- 管理员用户状态管理

### 生产环境建议
- 配合 Spring Security / 拦截器校验管理员角色
- 对封禁操作进行操作日志记录

