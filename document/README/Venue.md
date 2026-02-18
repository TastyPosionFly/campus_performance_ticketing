# 校园演出订票系统 - 场地（Venue）模块接口设计与使用说明

本模块为系统的场地（剧院、礼堂等）管理后端服务，**基于 Spring Boot Controller + Service + JPA 分层架构，支持场地信息管理、开放时段配置、屏蔽特殊日期与演出同步取消等核心能力**。本说明涵盖主要接口用法、参数结构及论文级设计亮点与研究价值总结。

---

## 一、系统分层设计简介

- **Controller（接口层）**：仅负责接收HTTP请求、参数解析、权限判定和响应包装，所有核心业务逻辑下沉至Service。
- **Service（业务层）**：组织参数校验、数据库操作、数据脱敏、图片处理、开放时段和屏蔽流程等所有关键业务。
- **DAO/Repository（数据访问层）**：面向实体的标准JPA增删查改接口，代码简洁易维护。
- **DTO（数据传输对象）**：基于业务场景对响应字段、图片、管理员信息进行脱敏与定制，保证敏感数据不外泄。
- **工具类（Util）**：包含图片文件、URL处理、JSON校验、资源物理清理等。

---

## 二、主要业务逻辑说明

- **场地创建/更新/删除**：仅超级管理员可操作，强制图片与设备信息校验，场地资源物理与逻辑双重删除；
- **场地开放时段管理**：支持周别开放/休息规则，批量设置并校验时间段合理性，逻辑严格避免空值或时段冲突；
- **场地屏蔽及演出同步取消**：管理员可屏蔽指定日期，并自动取消对应演出场次，事务保证同步与数据一致性；
- **高可拓展图片管理**：封面/轮播图支持文件与URL混合上传，落地本地存储方便统一资源管理，突破分布式部署障碍；
- **权限模型灵活安全**：用户分为超级管理员、场地管理员、普通用户，权限粒度可扩展适应更复杂场景。

---

## 三、接口使用说明概览

### 1. 创建场地

```
POST /api/venues
Content-Type: multipart/form-data
```
**请求数据（CreateVenueDto）：**

```json
{
  "name": "文艺剧院",
  "description": "学校大型演出场地",
  "address": "校区东路16号",
  "coverImageUrl": "http://example.com/cover.jpg",   // 或用coverImageFile文件流
  "photoUrlList": ["http://example.com/photo1.jpg", "http://example.com/photo2.jpg"], // 或用photoFiles文件流列表
  "capacity": 900,
  "type": 1,
  "equipmentInfo": "{\"灯光\":\"LED\",\"音响\":\"BOSE\"}",
  "managerId": 66
}
```
- 图片上传请通过`coverImageFile`（单文件）、`photoFiles[]`（多文件）提交。

**响应示例：**
```json
{ "success": true, "message": "创建场地成功: 文艺剧院" }
```
#### 设计说明
- 超级管理员专属，图片必需，设备信息格式强校验，图片自动下载入本地并落库，资料安全脱敏。

---

### 2. 获取场地详情

```
GET /api/venues/{id}
```
**请求数据：**
- 无参数，仅路径变量。

**响应示例：**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "name": "文艺剧院",
    "description": "...",
    "address": "...",
    "coverImage": "http://xxx/cover.jpg",
    "photoList": [
      { "id": "uuid1", "url": "http://xxx/photo1.jpg", "originalName": "file1.jpg" },
      { "id": "uuid2", "url": "http://xxx/photo2.jpg", "originalName": "file2.jpg" }
    ],
    "capacity": 900,
    "type": 1,
    "equipmentInfo": { "灯光": "LED", "音响": "BOSE" },
    "status": 1,
    "manager": { "id": 66, "name": "张三", "avatarUrl": "...", "phone": "13200000000" }
  }
}
```
#### 设计说明
- 响应结构脱敏，图片路径自动前缀拼接，管理员信息安全可用。

---

### 3. 场地列表查询

```
GET /api/venues
```
**请求数据（查询参数可选）：**
```json
{
  "name": "大剧院",
  "type": 1,
  "status": 1
}
```
（实际为QueryString参数，如：`/api/venues?name=大剧院&type=1&status=1`）

**响应结构同详情。**

---

### 4. 更新场地信息

```
POST /api/venues/update
Content-Type: multipart/form-data
```
**请求数据（UpdateVenueDto）：**

```json
{
  "id": 123,
  "name": "新名称",             // 可选
  "description": "新描述",      // 可选
  "address": "新地址",         // 可选
  "capacity": 1000,            // 可选
  "type": 2,                   // 可选
  "status": 1,                 // 可选
  "coverImageUrl": "http://xxx/newcover.jpg",  // 或coverImageFile文件
  "newPhotoUrlList": ["http://xxx/photo3.jpg"],         // 新增轮播图URL
  "newPhotoFiles": [附件],                                   // 新增轮播图文件
  "deletePhotoIds": ["uuid1"],                         // 要删除的图片ID
  "replacePhotoMap": "{\"uuid-old-1\":0}",             // 替换图片JSON（ID对应附件索引）
  "replaceFiles": [附件],                                  // 替换用的新文件
  "equipmentInfo": "{\"舞台\":\"新布局\"}",           // 可选
  "managerId": 77                                     // 可选
}
```

**响应示例：**
```json
{ "success": true, "message": "保存成功，但部分操作未生效：图片上传失败" }
```

---

### 5. 删除场地

```
DELETE /api/venues/{id}/delete
```
**请求数据：**
- 无参数，仅路径变量。

**响应示例：**
```json
{ "success": true, "message": "场地及相关资源已成功删除" }
```
---

### 6. 批量设置开放时间

```
POST /api/venues/{venueId}/hours
Content-Type: application/json
```
**请求数据（List<OpeningHoursDto>）：**
```json
[
  {
    "dayOfWeek": 1,
    "isClosed": false,
    "openTime": "08:30:00",
    "closeTime": "21:30:00"
  },
  {
    "dayOfWeek": 7,
    "isClosed": true
  }
]
```

**响应示例：**
```json
{ "success": true, "message": "设置成功" }
```

---

### 7. 屏蔽场馆并取消当天演出

```
POST /api/venues/block
Content-Type: application/json
```
**请求数据（BlockVenueRequestDto）：**

```json
{
  "venueId": 123,
  "blockedDate": "2026-02-10",
  "reason": "设备检修"
}
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "venueId": 123,
    "blockedDate": "2026-02-10",
    "reason": "设备检修",
    "canceledPerformancesCount": 2,
    "message": "场馆 (ID: 123) 已屏蔽日期 2026-02-10，并取消了 2 场演出"
  }
}
```
---

### 8. 获取场地开放与屏蔽信息

```
GET /api/venues/{venueId}/hours-and-blocks
```
**请求数据：**
- 无参数，仅路径变量。

**响应示例：**
```json
{
  "success": true,
  "data": {
    "openingHours": [
      { "dayOfWeek": 1, "isClosed": false, "openTime": "08:30:00", "closeTime": "21:30:00" }
    ],
    "blockedDates": ["2026-02-10", "2026-02-24"]
  }
}
```

### 8. 获取场地日程（场地内全部演出场次）

```
GET /api/venues/{venueId}/events?start=yyyy-MM-dd&end=yyyy-MM-dd
```

**请求参数：**
- `venueId`：路径变量，场馆ID。
- `start`：查询区间起始日期（字符串，格式：yyyy-MM-dd），必填。
- `end`：查询区间终止日期（字符串，格式：yyyy-MM-dd），必填。

**响应示例：**
```json
{
  "success": true,
  "data": [
    {
      "sessionId": 1001,
      "performanceId": 2001,
      "performanceName": "夏夜交响音乐会",
      "organizerName": "艺术团",
      "startTime": "2026-07-05T19:30:00",
      "endTime": "2026-07-05T21:00:00",
      "performanceDate": "2026-07-05"
    },
    {
      "sessionId": 1002,
      "performanceId": 2002,
      "performanceName": "青春舞会",
      "organizerName": "街舞协会",
      "startTime": "2026-07-06T18:00:00",
      "endTime": "2026-07-06T20:00:00",
      "performanceDate": "2026-07-06"
    }
  ]
}
```

#### 设计说明
- 支持任意日期区间检索，返回每场排期的sessionId、演出名、举办方、起止时间等核心信息，适用前端日历日程、场地管理、可视化等业务。
- 参数如格式或区间非法时返回友好错误提示（如日期格式错误、跨度过大等）。
- 响应结构简洁，所有时间字段为标准ISO格式字符串，组织者名称避免隐私泄露。
- 场景包括预约场地时冲突校验、运营后台排班展示、大屏日历等。

**错误响应示例：**
```json
{
  "success": false,
  "message": "日期格式错误，请使用 yyyy-MM-dd"
}
```
---

## 四、错误响应约定

所有接口均返回统一结构：
```json
{ "success": false, "message": "错误信息" }
```
前端可统一异常处理，便于论文接口结构论证。

---