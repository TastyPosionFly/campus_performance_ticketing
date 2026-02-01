# 演出浏览模块（Performance Browsing）接口设计与论文说明

本模块负责系统演出的推荐、热度、评论与媒体外链等浏览与交互功能，**整合人工推荐与自动榜单，实现多维度智能热点推荐，为用户提供个性化浏览体验。**  
支持“首页推荐”、“热度榜排行”、“评论互动”、“直播/回放链”等主要功能。特别适用于论文系统说明与创新点阐释。

---

## 一、系统分层与推荐算法亮点说明

- **Controller（接口层）**：仅负责接收HTTP请求、参数解析与转发至Service，所有业务规则/推荐算法下沉到Service层。
- **Service（业务层）**：
    - **推荐聚合服务**通过`RecommendationAggregationService`，优先融合人工（编辑/管理员设定）推荐和自动热度榜，解决冷启动与实时热门兼顾的问题。
    - 热度数据基于浏览量、分享量、评论数、售出票量、实际到场人数等多指标加权计算（可论文重点描述算法公式与权重设定）。
    - 支持推荐轮播、置顶推荐、热榜补位多种展现形式，“人工+自动”结合。
- **DAO/Repository（数据访问层）**：统一数据存储、索引优化、批量提取与聚合，保证推荐与榜单高速处理。
- **DTO/VO（数据传输对象/卡片）**：卡片式轻量输出，包含演出简要信息、统计数据、推荐标签等，适合前端高并发分页与流式加载。

---

## 二、主要接口设计说明与示例（参数与返回数据）

### 1. 获取混合推荐列表

```
GET /api/recommendation/list?type=1&limit=5
```

**请求参数说明：**
- `type`: 推荐位置（1-首页轮播、2-列表置顶）
- `limit`: 推荐数量（如5条）

**返回数据示例：**
```json
[
  {
    "id": 101,
    "title": "盛夏专场音乐会",
    "description": "毕业生专属年度巨献...",
    "posterUrl": "http://example.com/poster.jpg",
    "categoryId": 2,
    "publishStatus": 1,
    "statusDesc": "已发布",
    "createTime": "2026-06-01T08:10:23",
    "viewCount": 500,
    "shareCount": 67,
    "commentCount": 22,
    "hotScore": 1340.0,
    "recommendationTag": "官方推荐"
  },
  {
    "id": 102,
    "title": "光影电影回顾展",
    "description": "回顾经典光影，探寻心灵印象。",
    "posterUrl": "http://example.com/mov.jpg",
    "categoryId": 3,
    "publishStatus": 1,
    "statusDesc": "已发布",
    "createTime": "2026-06-03T19:20:38",
    "viewCount": 200,
    "shareCount": 5,
    "commentCount": 3,
    "hotScore": 351.5,
    "recommendationTag": "热度飙升"
  }
]
```

---

### 2. 上报浏览/分享量（埋点数据接口，方便论文举例用户行为反馈）

```
POST /api/recommendation/stats/view/101
POST /api/recommendation/stats/share/101
```
**请求参数：**
- 路径变量 `performanceId`。

**返回示例：**
```json
{ "success": true, "message": "上报成功" }
```

---

### 3. 评论管理接口

**发表评论**
```
POST /api/comment/post
Content-Type: application/json
```
```json
{
  "performanceId": 101,
  "content": "太震撼了！音乐好听，氛围超��"
}
```

**返回示例：**
```json
{ "success": true, "message": "评论成功" }
```

**获取评论列表**
```
GET /api/comment/list?performanceId=101&page=0&size=10
```
**返回示例：**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "content": "太震撼了！音乐好听，氛围超棒",
      "status": 1,
      "createTime": "2026-06-01T21:23:10",
      "userId": 6003,
      "nickname": "王同学",
      "avatarUrl": "http://example.com/u.png"
    }
    // ...分页内容
  ]
}
```

---

### 4. 媒体外链管理

**添加外链**
```
POST /api/media/add
Content-Type: application/json
```
```json
{
  "performanceId": 101,
  "type": 2,
  "platform": 1,
  "externalKey": "https://www.bilibili.com/video/BV1...',
  "title": "直播现场",
  "sortOrder": 1
}
```

**返回：**
```json
{ "success": true, "message": "外链上传成功" }
```

**获取外链列表**
```
GET /api/media/list?performanceId=101
```
```json
{
  "success": true,
  "data": [
    {
      "id": 1501,
      "performanceId": 101,
      "type": 2,
      "typeName": "在线直播",
      "platform": 1,
      "platformName": "Bilibili",
      "externalKey": "https://www.bilibili.com/video/BV1...",
      "title": "直播现场",
      "sortOrder": 1
    }
    // ...
  ]
}
```

---

## 四、错误响应标准

所有接口均标准返回：
```json
{ "success": false, "message": "错误信息" }
```
便于前后端一致处理与论文接口对比。

---