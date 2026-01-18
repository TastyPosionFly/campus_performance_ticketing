# 校园演出订票系统 - 组织模块接口设计与使用说明

本模块负责校园演出活动的组织管理相关后端服务，**基于 Spring Boot Controller + Service + JPA 分层架构**。  
此文档聚焦组织相关 API 的路由、数据结构、参数与权限、主要业务逻辑，以及适用于论文的技术和架构性描述。

---

## 一、系统分层设计简介

- **Controller（接口层）**：对外接收 RESTful 请求，参数解析、简单权限拦截，所有业务逻辑完全委托 Service。
- **Service（业务层）**：包含详细业务流程，实现权限判定、数据拼装、头像处理、申请审批等。
- **DAO/Repository**：负责底层实体对象的数据库查询和存储。
- **DTO（数据传输对象）**：将请求/响应数据结构公开或私有，保障接口安全。
- **工具类（Util）**：如图片路径处理、文件保存。

---

## 二、主要业务逻辑说明

- 组织创建采用申请+审批流程，防止组织被恶意创建。
- 每个组织有唯一首领，支持首领更换，由首领或管理员发起。
- 组织/成员资料分为公开和敏感字段，依据用户角色以及权限分级返回。
- 加入/解散组织、成员角色变更等涉及审批流程，及时反馈申请状态。
- 相册图片支持本地和网络两种上传方式，后台自动处理路径和权限。

---

## 三、接口使用说明（重点）

### 1. 申请创建组织

```
POST /api/organization/apply
Content-Type: multipart/form-data
```

**请求参数（ApplyOrganizationRequest DTO）：**
```json
{
  "orgName": "文艺社",
  "orgDescription": "学校文艺活动组织",
  "avatarUrl": "http://example.com/avatar.png"
}
```
**文件参数：**
- avatarFile: 图片文件（可选）

**响应参数：**
```json
{
  "success": true,
  "message": "创建申请已提交，等待审核"
}
```

**设计说明：**
- 提交时自动下载头像（如为URL）或保存上传文件，统一处理本地路径。
- 提交即生成申请记录，须管理员审批。

---

### 2. 更换组织首领

```
POST /api/organization/change-leader
```
**请求参数：**
- orgId: Long
- newLeaderId: Long

**响应：**
```json
{
  "success": true,
  "message": "组织首领更换成功"
}
```

**设计说明：**
- 权限判定，仅允许首领本人或管理员操作，严防越权。

---

### 3. 获取所有组织（不含待审核或已解散）

```
GET /api/organization/all
```

**响应数据结构：**
```json
{
  "success": true,
  "data": [
    {
      "id": 123,
      "name": "文艺社",
      "description": "学校文艺活动组织",
      "avatarUrl": "http://xxx/avatar.png",
      "leader": {
        "nickname": "小明",
        "avatar": "http://xxx/leader.png",
        "major": "艺术管理",
        "college": "管理学院",
        "status": 1
      },
      "status": 1,
      "statusDesc": "正常"
    }
  ]
}
```

**设计说明：**
- 仅返回活跃组织，便于前端列表展示。

---

### 4. 获取指定组织详情

```
GET /api/organization/{orgId}
```

**响应结构与“所有组织”单条一致。**

---

### 5. 提交组织解散申请

```
POST /api/organization/disband
```
**请求参数：**
- orgId: Long

**响应：**
```json
{
  "success": true,
  "message": "组织解散申请已提交，等待审核"
}
```

**设计说明：**
- 解散需审批，只允许首领或管理员发起。
- 高效防止组织被单人强制删除。

---

### 6. 获取某个组织的成员列表

```
GET /api/organization/{orgId}/members
```

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "user": {
        "nickname": "小王",
        "avatar": "http://xxx/u.png",
        "major": "音乐学",
        "college": "艺术学院",
        "status": 1
      },
      "memberRole": "MEMBER",
      "status": 1,
      "statusDesc": "已加入"
    }
  ]
}
```

---

### 7. 获取自己加入的组织列表

```
GET /api/organization/my-organizations
```

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "organization": { ...同上... },
      "memberRole": "MANAGER",
      "status": 1,
      "statusDesc": "已加入"
    }
  ]
}
```

---

### 8. 申请加入组织

```
POST /api/organization/member/apply
Content-Type: application/json
```

**请求参数（ApplyJoinOrganizationRequest DTO）：**
```json
{
  "orgId": 123,
  "reason": "热爱文艺，希望参与活动"
}
```
**响应：**
```json
{
  "success": true,
  "message": "申请已提交，等待审核"
}
```

**设计说明：**
- 已在组织的成员不能重复申请。
- 所有加入行为可追溯、可审批。

---

### 9. 更改成员身份（非直接首领变更）

```
POST /api/organization/member/change-role
Content-Type: application/json
```

**请求参数（ChangeMemberRoleRequest DTO）：**
```json
{
  "orgId": 123,
  "memberId": 456,
  "newRole": "MANAGER"
}
```
**响应：**
```json
{
  "success": true,
  "message": "成员身份更改成功"
}
```

**设计说明：**
- 管理角色变更，首领不能直接更改为LEADER，需专门流程。

---

### 10. 退出组织

```
POST /api/organization/member/quit
```

**请求参数：**
- orgId: Long

**响应：**
```json
{
  "success": true,
  "message": "成功退出组织"
}
```

**设计说明：**
- 组织首领不可直接退出，需转让首领角色后可退出。

---

### 11. 踢出组织成员

```
POST /api/organization/member/kick
```

**请求参数：**
- orgId: Long
- memberId: Long

**响应：**
```json
{
  "success": true,
  "message": "成功踢出组织成员"
}
```

**设计说明：**
- 仅首领具有踢人权限，防止越权。

---

### 12. 上传组织相册图片

```
POST /api/organization/album/upload
Content-Type: multipart/form-data
```

**请求参数（UploadAlbumPhotoRequest DTO）：**
```json
{
  "organizationId": 123,
  "photoUrl": "http://xxx/photo.jpg",
  "description": "活动现场"
}
```
- photoFile: 图片文件（可选）

**响应：**
```json
{
  "success": true,
  "message": "图片上传成功"
}
```

**设计说明：**
- 仅LEADER/MANAGER可上传图片，严格校验。
- 支持本地和网络图片，统一存储。

---

### 13. 删除组织相册���片

```
POST /api/organization/album/delete
```
**请求参数：**
- photoId: Long

**响应：**
```json
{
  "success": true,
  "message": "图片删除成功"
}
```

---

### 14. 查询组织相册图片列表

```
POST /api/organization/album/list
```
**请求参数：**
- organizationId: Long

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "uploader": {
        "nickname": "小明",
        "avatar": "http://xxx/upl.png",
        "major": "艺术管理",
        "college": "管理学院"
      },
      "photoUrl": "http://xxx/photo.jpg",
      "uploadTime": "2026-01-18T12:00:00"
    }
  ]
}
```

---

## 四、错误响应约定

所有接口均使用统一 `ApiResponse` 数据结构，错误均返回：
```json
{
  "success": false,
  "message": "错误描述"
}
```
前端可按 `success` 字段统一处理业务流程。

---