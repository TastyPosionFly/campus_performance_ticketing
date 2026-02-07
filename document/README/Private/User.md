# 校园演出订票系统后端 API 使用与设计说明

本项目为校园演出订票系统后端服务，**基于 Spring Boot、JPA 架构**，支持多角色分层的用户认证、权限与资料管理。  
**本页面同时聚焦接口使用与相关设计说明，方便论文编写和系统分析。**

---

## 一、系统分层设计简介

- **Controller（接口层）**：仅负责接收HTTP请求、参数解析、权限校验、结果响应。业务全部委托 Service 层。
- **Service/Logic（业务层）**：承载所有业务流程和数据库操作，包括权限判断、数据脱敏、头像处理等。
- **DAO/Repository（数据访问层）**：数据库实体和查询方法（JPA标准）。
- **DTO（数据传输对象）**：用于控制响应字段的范围，公开信息与管理员信息分离，便于权限与数据控制。
- **工具类（Util）**：如头像URL拼接，路径处理等。

---

## 二、主要业务逻辑说明

- 用户注册/登录流程：首次登录自动注册，支持开放图片链接自动下载本地头像（避免网络头像丢失），用户数据库记录会更新。
- 用户信息展示：**分角色呈现**；普通用户仅能获取公开资料，管理员可见全部字段。
- 头像处理：系统优先本地头像路径，若为网络链接则自动拼接，并按需下载/缩放。
- 用户个人信息修改、资料更新支持多种字段，包括昵称、学号、学院、专业、手机号、头像等。
- 管理员接口包括用户封禁、用户列表、角色变更等权限操作，所有敏感业务均在 Service 层实现并严格校验。

---

## 三、接口使用说明（重点）

### 1. 用户登录/注册（微信认证）

```
POST /api/auth/login
Content-Type: application/json
```
**请求示例：**
```json
{
  "code": "wx_login_code_abc123",           // 微信小程序 wx.login() 返回的 code
  "openid": "o123456abcde",                 // 前端获取的 openid（可选，用于一致性校验）
  "nickname": "小王",                       // 用户昵称
  "avatar": "https://thirdwx.qlogo.cn/..."  // 微信头像URL，可选
}
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9....",
    "userId": 222,
    "openid": "o123456abcde",
    "nickname": "小王",
    "avatar": "http://example.com/avatars/222.jpg",
    "role": "USER",
    "status": 1
  },
  "message": "登录成功"
}
```
---

### 2. 获取用户信息（分角色返回）

路径：`GET /api/users/member?openId={openid}`

权限与数据分级响应：

- **普通用户**：  
  只获得公开信息（昵称、头像、学院、专业），数据以 PublicUserInfo DTO 格式返回；
- **管理员（ADMIN/SUPER_ADMIN）**：  
  获得完整用户信息。

**公开信息示例：**
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
**完整信息示例：**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "openid": "openid_001",
    "nickname": "Alice",
    "avatar": "...",
    "role": "USER",
    "college": "信息学院",
    "major": "软件工程",
    "studentNo": "20260001",
    "phone": "132****0001",
    "userIdentity": 1,
    "status": 1,
    "createTime": "2026-01-01T10:00:00",
    "updateTime": "2026-01-05T12:00:00",
    "lastLoginTime": "2026-01-13T13:22:00"
  }
}
```

**设计说明**：  
利用DTO分离公开与敏感信息、在业务层（Service）统一判断并封装，极大提升了安全性和代码维护性，同时有助于论文的权限模型说明。

---

### 3. 用户资料更新

路径：`PUT /api/users/profile`
- 支持头像上传（图片文件）、昵称、学号、专业、手机号等变更
- 头像自动缩放为统一尺寸（200x200）
- 更新头像时会自动删除服务器中的旧头像文件

**请求格式：**`multipart/form-data`

**请求参数：**
```json
{
  "nickname": "张三",              // 可选，用户昵称
  "avatarFile": "(文件)",          // 可选，头像图片文件
  "userIdentity": 1,              // 可选，用户身份（1=学生，2=教师等）
  "studentNo": "20260001",        // 可选，学号
  "major": "软件工程",             // 可选，专业
  "college": "信息学院",           // 可选，学院
  "phone": "13800138000"          // 可选，手机号
}
```
**响应示例：**
```json
{
"success": true,
"data": {
"id": 123,
"nickname": "张三",
"avatar": "http://example.com:8080/app/data/avatar/7b5a9cf3-e91f-4c0e-bc14-c4a28382486d.jpg",
"userIdentity": 1,
"studentNo": "20260001",
"major": "软件工程",
"college": "信息学院",
"phone": "13800138000",
"status": 1
},
"message": "操作成功"
}
```
---

**设计说明**：  
本接口最大程度提升用户体验，支持灵活头像来源（上传、网络），自动处理图片规格。同时业务全部在 Service 层处理，代码分层清晰，便于安全与扩展说明。

---

### 4. 管理员接口（权限控制）

#### 4.1 用户封禁/解封

路径：`PUT /api/admin/users/ban?openId={openid}&ban={true|false}`  
权限：仅ADMIN/SUPER_ADMIN

**响应：**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "status": 0
  }
}
```

**设计说明**：  
敏感操作由拦截器保障，只能管理员访问，业务交由 Service 统一实现。

#### 4.2 查询全部用户

路径：`GET /api/admin/users/list`  
权限：仅ADMIN/SUPER_ADMIN
- 返回全部 UserInfo 对象列表

#### 4.3 更改用户角色

路径：`PUT /api/admin/users/role?openId={openid}&newRole={role}`  
权限：仅SUPER_ADMIN
- `newRole` 允许且仅允许 `"USER"`, `"VENUE_ADMIN"`, `"ADMIN"`, `"SUPER_ADMIN"`（**严格区分大小写**），否则业务层直接拒绝。

**请求示例：**
```http
PUT /api/admin/users/role?openId=openid_002&newRole=ADMIN
```
**响应：**
```json
{
  "success": true,
  "data": {
    "userId": 456,
    "role": "ADMIN"
  }
}
```

**设计说明**：  
角色变更操作在 Service 层充分校验，减少数据污染与权限泄漏。

---

### 错误响应约定

所有接口错误均返回如下结构：
```json
{
  "success": false,
  "message": "错误描述..."
}
```
便于前端统一处理，也便于论文接口异常对比说明。

---

## 四、系统设计亮点与论文写作指引

- **分层架构明确**：Controller仅参数转发，核心业务/校验/数据聚合在Service层完成，DAO专注数据访问，DTO实现响应动态分级。
- **权限模型安全**：敏感操作统一在业务层校验，接口层零逻辑。
- **头像处理智能**：自动区分、下载、拼接，兼容网络和本地，有助于说明图片资源管理流程。
- **响应安全可靠**：接口响应按权限严格分级，减少敏感信息泄漏，提高论文安全性评估分数。
- **扩展友好**：所有业务拓展只需修改 Service/DTO，Controller 与数据库结构解耦，便于论文扩展部分撰写。
- **严格参数校验**：比如角色参数大小写完全一致、头像路径严谨处理。

---

## 五、推荐论文相关描述片段

- “本系统后端采用分层架构，接口层不实现业务逻辑，仅作为参数转发，由业务层统一实现所有权限判断和数据操作，保证了代码安全性与可维护性，并方便未来功能扩展。”
- “针对用户信息保护，采用 DTO 分级响应机制，管理员可见所有字段，普通用户仅能获取公开信息，有效防止敏感数据泄漏。”
- “系统自动处理用户头像上传和网络下载，统一存储规则并自动拼接可访问 URL，提升了资源管理效率与前端一致性。”

---

如需更详细字段列表、代码引用或论文撰写模板，请联系后端开发或查阅各接口源码注释。