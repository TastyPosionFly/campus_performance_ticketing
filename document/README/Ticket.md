# 校园演出订票系统 - 票务管理（Ticket）模块接口设计与使用说明

本模块负责系统的票务预约、出票、核销、模板管理等核心服务，**基于Spring Boot分层架构，包含票据预订、电子票模板上传/更新、检票核销和到场统计等主要功能**。  
适用于论文系统设计说明、接口文档与工程分层架构分析，可直接用于论文书写。

---

## 一、系统分层设计简介

- **Controller（接口层）**：负责接收、解析HTTP请求、校验参数并转发至Service层，接口功能单一无业务逻辑。
- **Service（业务层）**：核心业务规则、参数校验、权限判定、票据生成与核销、资源存储与模板管理全在此层实现。
- **DAO/Repository（数据访问层）**：承担实体对象的数据增删查改和多条件/批量查询，保证数据获取高效与规范。
- **DTO（数据传输对象）**：用于响应票据信息、预约与模板的安全数据脱敏和结构化输出，确保数据安全。
- **工具类（Util）**：图片与核销码生成、文件存储、地址拼接、定时失效等通用组件。

---

## 二、主要业务逻辑亮点

- **票据预约防止超卖与重复预定**：同一场次同一用户限制一张票并防止库存被抢光，保证了公平和准确。
- **扫码核销权限分级**：只有超级管理员和场地管理员可扫码核验，安全性强，杜绝越权检票。
- **模板管理高度自动化**：电子票背景图本地上传与主键自动维护，可批量关联多个场次、灵活上下架。
- **到场统计全链路打通**：一键查询指定场次已核销用户，管理员/组织者均可权限认证，结果统计方便论文分析。
- **定时任务自动失效**：场次结束后未核销票自动失效并结算，保证系统数据时效和场次闭环。
- **错误与异常友好反馈**：所有接口均固定格式响应异常，为前端和论文范文树立良好标准。

---

## 三、主要接口与请求/响应示例

### 1. 预约/抢票

```
POST /api/ticket/book
Content-Type: application/json
```
**请求数据：**
```json
{
  "sessionId": 888
}
```
**响应数据：**
```json
{
  "success": true,
  "data": {
    "id": 1234,
    "ticketCode": "CODE20260201ABCDE",
    "status": 0,
    "statusText": "已预约",
    "performanceTitle": "夏季音乐会",
    "performancePosterUrl": "http://example.com/poster.jpg",
    "sessionId": 888,
    "venueName": "文艺剧院",
    "venueAddress": "校区东路16号",
    "startTime": "2026-07-15T19:00:00",
    "endTime": "2026-07-15T21:00:00",
    "bookingTime": "2026-06-18T10:11:12"
  },
  "message": "预约成功"
}
```

---

### 2. 查询票夹列表（分页）

```
GET /api/ticket/my?page=0&size=10&status=0
```
**请求参数：**
- page、size 分页参数
- status 可选（0-已预约 1-已核销等）

**响应数据为Page<TicketDetailDTO>。**

---

### 3. 查询单张票据详情

```
GET /api/ticket/{ticketId}
```
**响应数据：**
```json
{
  "success": true,
  "data": {
    "id": 1234,
    "ticketCode": "...",
    "status": 1,
    "statusText": "已核销",
    "performanceTitle": "夏季音乐会",
    "venueName": "文艺剧院",
    "startTime": "2026-07-15T19:00:00",
    "endTime": "2026-07-15T21:00:00",
    "checkInTime": "2026-07-15T18:50:00"
  }
}
```

---

### 4. 管理员扫码检票/核销

```
POST /api/ticket/check-in?ticketCode=CODE20260201ABCDE
```
- 仅超级管理员/场地管理员可操作，其他用户无权限。

**响应结果：**
```json
{ "success": true, "message": "核销完成" }
```
或
```json
{ "success": false, "message": "票据已核销或无权限" }
```

---

### 5. 上传/更新电子票模板

```
POST /api/ticket/template/upload
Content-Type: multipart/form-data
```
**请求数据（TicketTemplateUploadDTO）：**
```json
{
  "sessionIds": [888, 889],
  "status": 1
}
```
- imageFile：电子票模板图片文件

**响应：**
```json
{ "success": true, "message": "电子票模板上传成功" }
```

---

### 6. 获取场次电子票背景图URL

```
GET /api/ticket/template/url/{sessionId}
```
**响应：**
```json
{ "success": true, "data": "http://example.com/ticket-bg.jpg" }
```

---

### 7. 查询实际到场人员名单

```
GET /api/ticket/attendance/{sessionId}
```
**响应：**
```json
{
  "success": true,
  "data": [
    {
      "userId": 102,
      "nickname": "小明",
      "avatar": "http://example.com/u.png",
      "major": "钢琴",
      "college": "艺术学院",
      "status": 1,
      "userIdentity": 1,
      "userIdentityDesc": "学生",
      "studentNo": "20261234",
      "checkInTime": "2026-07-15T19:05:00"
    }
    // ...
  ]
}
```

---

## 四、错误响应约定

所有接口均返回统一结构：
```json
{ "success": false, "message": "错误信息" }
```

---