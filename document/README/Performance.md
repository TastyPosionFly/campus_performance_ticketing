# 校园演出订票系统 - 演出（Performance）模块接口设计与使用说明

本模块负责校园演出活动的申请、管理与查询等核心服务。**基于 Spring Boot Controller + Service + JPA 分层架构，实现演出申请审批、场次冲突校验、演职人员管理、海报与图片上传、分页检索等主要能力。**  
适合论文系统设计、接口说明和工程文档引用。

---

## 一、系统分层设计简介

- **Controller（接口层）**：接收HTTP请求、参数解析、权限提取，所有业务逻辑委托Service，接口清晰单一。
- **Service（业务层）**：完成参数校验、权限判断、演出/场次冲突判断、数据操作与审批等重要业务流。
- **DAO/Repository（数据访问层）**：JPA实体的增删查改接口，事务与复杂查询独立实现。
- **DTO（数据传输对象）**：演出详情、申请、场次、演职人员等各类数据结构脱敏定制，保证接口安全。
- **工具类（Util）**：图片资源管理（上传/下载/路径处理）、物理清理等。

---

## 二、主要业务逻辑亮点

- **多角色演出申请与审批流程**：支持个人和社团作为演出举办方，结合系统用户和组织权限管控。
- **场次冲突与屏蔽校验**：所有场次申请均校验场馆闭馆时间和时间冲突，保证演出安排合理有效。
- **演职人员管理**：支持演职人员关联系统用户或自定义输入，照片上传与路径管理自动化。
- **海报/图片上传与路径处理**：资源自动存储本地，支持前端多文件/URL混合模式，路径前缀统一。
- **权限模型健全**：演出主办人、管理员拥有不同操作权限，业务规则可拓展。
- **分页查询与条件检索**：演出内容支持关键词、分类、状态多条件检索与分页，方便前后端集成和论文数据展示。

---

## 三、接口使用说明（带请求JSON示例）

### 1. 提交演出申请

```
POST /api/performance/apply
Content-Type: multipart/form-data
```

**请求数据（CreatePerformanceCmd）：**
```json
{
  "title": "夏季音乐会",
  "description": "由学生乐团举办的专场演出",
  "posterUrl": "http://example.com/poster.jpg",  // 或对应poster文件流
  "categoryId": 2,
  "organizerType": "USER",  // 或 "ORGANIZATION"
  "organizerId": 101,
  "sessions": [
    {
      "venueId": 1,
      "startTime": "2026-07-15T19:00:00",
      "endTime": "2026-07-15T21:00:00",
      "ticketTotal": 300
    }
  ],
  "staffList": [
    {
      "staffName": "王乐",
      "staffType": "指挥",
      "staffAvatar": "wang.jpg",  // 对应staffPhotos里的文件名
      "introduction": "学生乐团指挥"
    }
  ],
  "applyReason": "校内学生艺术团体音乐推广"
}
```

- `poster`为海报图片文件
- `staffPhotos[]`为演职人员定妆照列表

**响应示例：**
```json
{ "success": true, "message": "演出申请提交成功，等待管理员审批" }
```
#### 设计说明
- 场次、场馆、闭馆冲突、权限一站式校验，所有图片处理自动本地化，数据安全可追溯。

---

### 2. 修改演出信息

```
PUT /api/performance/update
Content-Type: multipart/form-data
```
**请求数据（UpdatePerformanceRequestDto）：**
```json
{
  "performanceCmd": {
    "performanceId": 666,
    "title": "夏季音乐会-变更标题",
    "description": "音乐演出变更",
    "posterUrl": "http://example.com/newposter.jpg",  // 或newPosterFile文件流
    "publishStatus": 1
  },
  "sessions": [
    {
      "venueId": 1,
      "startTime": "2026-07-18T20:00:00",
      "endTime": "2026-07-18T22:00:00",
      "ticketTotal": 300
    }
  ],
  "staffList": [
    {
      "staffName": "李晓",
      "staffType": "钢琴",
      "staffAvatar": "lixiao.jpg", // 对应staffPhotoFiles里的文件
      "introduction": "钢琴演奏员"
    }
  ],
  "delayReason": "天气原因延期"
}
```
- `newPosterFile`为新海报图片文件
- `staffPhotoFiles[]`为演职人员新照片文件列表

**响应示例：**
```json
{ "success": true, "data": { /* PerformanceDetailDto */ }, "message": "演出更新成功" }
```

---

### 3. 演出列表分页检索

```
GET /api/performance/list?keyword=音乐会&categoryId=2&status=1&page=0&size=10
```

**请求参数：**
- keyword: 标题/描述关键词（可选）
- categoryId: 演出类别（可选）
- status: 发布状态（可选，建议1-已发布）
- page/size: 分页参数（默认0/10）

**响应结构：**
- Page<PerformanceDetailDto>，每项内容包含演出基本信息、场次与演职人员简要结构、图片URL完整。

---

### 4. 获取演出详情

```
GET /api/performance/{id}
```
**请求参数：**
- 演出ID（路径变量）

**响应示例：**
```json
{
  "success": true,
  "data": {
    "performanceId": 999,
    "title": "夏季音乐会",
    "description": "一年一度的音乐盛会",
    "posterUrl": "http://example.com/poster.jpg",
    "publishStatus": 1,
    "statusDesc": "已发布",
    "sessions": [
      { "venueId": 1, "startTime": "2026-07-18T20:00:00", "endTime": "2026-07-18T22:00:00", "ticketTotal": 300 }
    ],
    "staff": [
      { "staffName": "李晓", "staffType": "钢琴", "staffAvatar": "http://example.com/staff.jpg", "introduction": "钢琴演奏员" }
    ]
  }
}
```

---

## 四、错误响应约定

所有接口均返回统一结构：
```json
{ "success": false, "message": "错误信息" }
```
统一前端处理，方便论文接口结构论证。

---