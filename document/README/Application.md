# 校园演出订票系统 - 申请与审批模块接口设计与使用说明

本模块负责后端“组织相关操作申请与审批”服务，**基于 Spring Boot Controller + Service 分层架构**。  
文档聚焦申请模块的主要业务接口、数据结构、参数与权限模型，并给出论文适用的架构特性描述。

---

## 一、系统分层设计简介

- **Controller（接口层）**：负责REST接口路径，方法分发，参数收集，无业务逻辑。
- **Service（业务层）**：业务流转，包括权限判断、参数校验、申请流程、审批批量处理等。
- **DTO（数据传输对象）**：申请类与审批类数据严格DTO分离，敏感字段脱敏。
- **DAO/Repository**：JPA实体操作，支持事务与批量操作，接口全部解耦于数据库实现。

---

## 二、主要业务逻辑说明

- 支持组织创建、成员加入、组织解散等所有相关操作的“申请-审批”机制，审批分层、流程清晰。
- 是否具有操作权限（如管理员/首领）、是否可批量处理、多状态流转（待审核/通过/拒绝/撤销）均由业务层控制。
- 支持批量审批，保证单条审核失败不影响其余申请。
- 所有申请实体、审批指令、审批结果等均有专属DTO数据结构。

---

## 三、接口使用说明（重点）

### 1. 查询本人（当前用户）提交的申请记录

```
GET /api/application/my-applications?applicationType={TYPE}
```

**请求参数：**
- applicationType（可选）：字符串，如 `"CREATE_ORG"`, `"JOIN_ORG"`, `"DISBAND_ORG"`

**响应结构：**
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "applicant": {
        "nickname": "Alice",
        "avatar": "http://xxx.png",
        "college": "信息学院",
        "major": "软件工程",
        "status": 1
      },
      "applicationType": "CREATE_ORG",
      "targetId": 201,
      "extraData": "{\"orgName\":\"文艺社\"}",
      "status": 1,
      "statusDesc": "待审核",
      "applyTime": "2026-01-16T10:00:00",
      "approveTime": null
    }
  ]
}
```

**设计说明：**
- 只返回当前用户创建的申请，数据与业务自动脱敏。

---

### 2. 查询所有申请（管理员或首领权限）

```
GET /api/application/list?applicationType={TYPE}&status={状态}
```

**请求参数：**
- applicationType（可选）
- status（可选）：1-待审核，2-通过，3-拒绝，4-撤销

**响应结构：**
```json
{
  "success": true,
  "data": [
    {
      "applicationId": 101,
      "applicationType": "JOIN_ORG",
      "applicantOpenId": "openid_001",
      "applicantName": "Alice",
      "applyTime": "2026-01-16T10:00:00",
      "status": 1,
      "statusDesc": "待审核",
      "targetId": 123,
      "extraData": "{\"orgName\":\"音乐社\"}",
      "displayTitle": "加入音乐社",
      "displayDescription": "想加入"
    }
  ]
}
```

**设计说明：**
- 管理员可查全部，组织首领仅能查本组织的“加入组织”类申请；
- 分层权限自动控制，非授权直接响应失败。

---

### 3. 批量审批申请（同意/拒绝）

```
POST /api/application/batch-review
Content-Type: application/json
```

**请求参数（List<ApplicationAuditCommand DTO>）：**
```json
[
  { "applicationId": 101, "newStatus": 2, "reason": "同意加入" },
  { "applicationId": 102, "newStatus": 3, "reason": "理由不充分" }
]
```

**响应结构：**
```json
{
  "success": true,
  "message": "审核成功1条，失败1条。申请102: 权限不足;"
}
```

**设计说明：**
- 单条审核独立事务，部分失败不影响整体；
- 业务层对参数有效性和操作人权限均做严格校验。

---

### 4. 撤销个人申请

```
POST /api/application/revoke?applicationId={ID}
```

**请求参数：**
- applicationId: Long

**响应结构：**
```json
{
  "success": true,
  "message": "成功撤销申请"
}
```

**设计说明：**
- 仅申请人本人可撤销，且仅可撤销“待审核”状态。
- 错误自动返回结构、便于前端/论文分析处理。

---

## 四、相关数据结构

### ApplicationPublicDto
```json
{
  "id": 101,
  "applicant": { ...PublicUserInfo... },
  "applicationType": "JOIN_ORG",
  "targetId": 123,
  "extraData": "{...}",
  "status": 1,
  "statusDesc": "待审核",
  "applyTime": "2026-01-16T10:00:00",
  "approveTime": null
}
```
### PendingApplicationDto
```json
{
  "applicationId": 101,
  "applicationType": "CREATE_ORG",
  "applicantOpenId": "openid_002",
  "applicantName": "Bob",
  "applyTime": "2026-01-17T09:00:00",
  "status": 2,
  "targetId": 321,
  "extraData": "{\"orgName\":\"舞台社\"}",
  "displayTitle": "加入舞台社",
  "displayDescription": "舞台表演经历"
}
```

### ApplicationAuditCommand
```json
{
  "applicationId": 101,
  "newStatus": 2,
  "reason": "审批同意"
}
```

---

## 五、错误响应约定

所有接口错误均返回：
```json
{
  "success": false,
  "message": "错误描述"
}
```
前端与论文均可统一异常处理。

---