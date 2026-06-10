# 系统架构设计

> 文档版本：v1.0
> 最后更新：2026-06-10

---

## 一、总体架构

### 1.1 架构概览

本项目采用经典的 **B/S 架构**（前后端半分离），后端使用 Spring Boot 框架，前端使用 Thymeleaf 模板 + 原生 JavaScript。实时通信基于 **WebSocket + STOMP** 协议。

```
┌─────────────────────────────────────────────────────────┐
│                      浏览器 (Browser)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   HTML 页面  │  │     CSS     │  │  JavaScript │    │
│  │  (Thymeleaf) │  │  (原生 CSS3) │  │  (Vanilla)  │    │
│  └──────┬──────┘  └─────────────┘  └──────┬──────┘    │
│         │                                  │            │
│         │    SockJS / STOMP Client        │            │
│         └──────────────┬──────────────────┘            │
└────────────────────────┼──────────────────────────────────┘
                         │ HTTP / WebSocket
                         │
┌────────────────────────▼──────────────────────────────────┐
│                   Spring Boot 应用                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Interceptor  │  │  Controller  │  │  WebSocket   │  │
│  │  (登录校验)  │  │   (REST API) │  │  (STOMP)     │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         └──────────────┬──┴──────────────┬──┘           │
│                        │                 │              │
│                ┌───────▼──────┐  ┌───────▼──────┐       │
│                │   Service    │  │  WebSocket   │       │
│                │   业务层     │  │  Handler     │       │
│                └───────┬──────┘  └───────┬──────┘       │
│                        │                 │              │
│                ┌───────▼─────────────────▼──────┐       │
│                │           Mapper (MyBatis)      │       │
│                └───────────────┬─────────────────┘       │
│                                │                          │
│                ┌───────────────▼─────────────────┐       │
│                │         MySQL Database          │       │
│                └──────────────────────────────────┘       │
└───────────────────────────────────────────────────────────┘
```

### 1.2 技术选型理由

| 技术 | 选型理由 |
|------|---------|
| Spring Boot 2.7.18 | 成熟稳定，社区活跃，学习资源丰富 |
| MyBatis | SQL 灵活可控，适合课程作业直观展示 |
| Thymeleaf | 服务端模板，与 Spring Boot 无缝集成，简化 demo |
| 原生 JS | 无前端框架依赖，部署简单 |
| WebSocket | 实时通信首选协议，浏览器原生支持 |
| STOMP | WebSocket 之上的轻量级消息协议 |
| SockJS | WebSocket 降级方案，兼容老旧浏览器/代理 |
| MySQL | 最流行的关系型数据库，课程通用 |
| Druid | 自带监控面板，方便调试 |

---

## 二、模块划分

### 2.1 后端模块（按功能）

```
com.chat
├── common          通用工具
│   ├── Result           统一返回结果
│   ├── GlobalExceptionHandler  全局异常处理
│   └── UserSession      Session 工具
│
├── config          配置
│   ├── WebConfig             Web MVC 配置
│   ├── WebSocketConfig       WebSocket 配置
│   └── UploadPathConfig      上传路径配置
│
├── interceptor     拦截器
│   └── LoginInterceptor      登录拦截
│
├── controller      控制器
│   ├── UserController        用户接口
│   ├── FriendController      好友接口
│   ├── GroupController       群组接口
│   ├── MessageController     消息接口
│   ├── FileController        文件接口
│   ├── PageController        页面跳转
│   └── ChatWebSocketController  WebSocket 消息
│
├── service         业务层
│   ├── UserService
│   ├── FriendService
│   ├── GroupService
│   ├── PrivateMessageService
│   └── FileService
│
├── mapper          MyBatis Mapper
│   ├── UserMapper
│   ├── FriendMapper
│   ├── FriendRequestMapper
│   ├── FriendGroupMapper
│   ├── ChatGroupMapper
│   ├── GroupMemberMapper
│   ├── GroupInviteMapper
│   ├── PrivateMessageMapper
│   ├── GroupMessageMapper
│   └── FileMapper
│
├── entity          实体类
│   ├── User, Friend, FriendRequest, FriendGroup
│   ├── ChatGroup, GroupMember, GroupInvite
│   ├── PrivateMessage, GroupMessage
│   └── File
│
└── dto             数据传输对象
    ├── FriendRequestDTO, FriendHandleDTO
    ├── FriendGroupCreateDTO, FriendMoveDTO
    ├── GroupCreateDTO, GroupJoinDTO
    └── ...
```

### 2.2 前端模块

```
src/main/resources/
├── templates/                  Thymeleaf 页面
│   ├── login.html              登录页
│   ├── register.html           注册页
│   └── chat.html               聊天主页面
│
└── static/                     静态资源
    ├── css/
    │   ├── common.css          全局样式
    │   └── chat.css            聊天页样式
    ├── js/
    │   ├── sockjs.min.js       SockJS 客户端
    │   ├── stomp.min.js        STOMP 客户端
    │   └── chat.js             业务逻辑
    └── img/                    图片资源
        ├── default-avatar.png  默认头像
        └── default-group.png   默认群头像
```

---

## 三、核心流程

### 3.1 用户登录流程

```
┌──────┐                  ┌────────┐               ┌─────────┐
│用户  │                  │前端    │               │后端     │
└──┬───┘                  └───┬────┘               └────┬────┘
   │ 1. 输入用户名密码         │                          │
   ├─────────────────────────▶│                          │
   │                          │ 2. POST /api/user/login  │
   │                          ├─────────────────────────▶│
   │                          │                          │ 3. 校验密码
   │                          │                          │    (BCrypt)
   │                          │                          │
   │                          │ 4. {code:200, data:user} │
   │                          │◀─────────────────────────┤
   │                          │ 5. 保存 session          │
   │                          │ 6. 跳转 /chat            │
   │                          │ 7. WebSocket 订阅        │
   │                          │    /topic/notification/  │
   │                          │    /topic/private/       │
   │                          │    /topic/user/status    │
   │                          │                          │
```

### 3.2 私聊消息流程

```
┌─────┐                              ┌─────┐
│ A   │                              │ B   │
└──┬──┘                              └──┬──┘
   │ 1. 输入消息 "你好"                 │
   │    点发送                         │
   │ 2. 调 REST API 或 WebSocket       │
   ▼                                  │
┌────────────────┐                     │
│ PrivateMessage │                     │
│ Service        │                     │
│ 3. 写入数据库  │                     │
│ 4. 推送 STOMP  │                     │
│ /topic/private/├──────WebSocket─────▶│
│   {B.id}       │                     │ 5. 收到消息
│                │                     │    onMessageReceived()
│                │                     │ 6. 渲染到聊天区
└────────────────┘                     │
   │                                  │
   ▼                                  │
   7. 自己也渲染这条消息                 │
```

### 3.3 好友申请流程

```
┌─────┐                ┌─────┐                ┌─────┐
│ A   │                │后端 │                │ B   │
└──┬──┘                └──┬──┘                └──┬──┘
   │ 1. POST /api/friend/request (toUserId=B)   │
   ├──────────────────────▶│                     │
   │                       │ 2. 写入 friend_request
   │                       │ 3. 推送 /topic/notification/{B.id}
   │                       │    {type: friend_request}
   │                       ├────WebSocket────────▶│
   │                       │                     │ 4. 收到通知
   │                       │                     │ 5. alert + 刷新列表
   │ 6. POST /api/friend/handle (agree)         │
   │   {requestId, status: 1}                    │
   ├──────────────────────▶│                     │
   │                       │ 7. 双向插入 friend 记录
   │                       │ 8. 删除 friend_request
   │                       │ 9. 推送 /topic/notification/{A.id}
   │                       │    {type: friend_request_result, status: 1}
   │                       ├────WebSocket────────▶│ (实际给 A)
   │ 10. 收到通知          │                     │
   │◀──────────────────────┤                     │
   │                       │                     │
```

### 3.4 WebSocket 消息流

```
┌──────────────┐                          ┌──────────────┐
│ Client A     │                          │ Client B     │
└──────┬───────┘                          └──────┬───────┘
       │                                         │
       │ STOMP CONNECT                           │
       ├──────────────────┐                      │
       │                  │                      │
       │                  ▼                      │
       │         ┌──────────────────┐            │
       │         │ WebSocketServer  │            │
       │         │ HandshakeInter.. │            │
       │         │ (JWT/Session校验) │            │
       │         └────────┬─────────┘            │
       │                  │                      │
       │ SUBSCRIBE         │                      │
       │ /topic/private/A  │                      │
       │ /topic/notification/A                     │
       │ /topic/user/status                      │
       │                  │                      │
       │◀────STOMP FRAME (CONNECTED)─────────────┤
       │                                         │
       │  A 发消息给 B                            │
       │ SEND /app/private {toId: B, content}    │
       ├──────────────────▶                      │
       │                  │                      │
       │                  ▼                      │
       │         ┌──────────────────┐            │
       │         │ ChatWebSocket    │            │
       │         │ Controller       │            │
       │         │ 收到消息后:      │            │
       │         │  1. 写库         │            │
       │         │  2. 推送给 B     │            │
       │         └────────┬─────────┘            │
       │                  │                      │
       │                  │ MESSAGE              │
       │                  │ destination:         │
       │                  │ /topic/private/B     │
       │                  ├─────────────────────▶│
       │                  │                      │ 收到消息
       │                  │                      │
       │                  │ MESSAGE              │
       │                  │ destination:         │
       │                  │ /topic/private/A     │
       │◀─────────────────┤ (回执/同步)           │
       │                  │                      │
```

---

## 四、安全设计

### 4.1 认证机制

- 基于 **HttpSession** 的会话管理
- 登录成功后，服务端将 User 对象存入 `session.setAttribute("loginUser", user)`
- 后续请求携带 `JSESSIONID` Cookie，服务端从 Session 中取用户

### 4.2 鉴权拦截

[LoginInterceptor.java](../src/main/java/com/chat/interceptor/LoginInterceptor.java) 拦截所有 `/api/**` 请求（除登录/注册外），未登录返回 401。

```java
if (session.getAttribute("loginUser") == null) {
    return Result.error(401, "未登录");
}
```

### 4.3 密码加密

使用 **BCrypt**（Spring Security Crypto）：

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode(rawPassword);  // 加密
boolean match = encoder.matches(rawPassword, hashed);  // 校验
```

- 不可逆
- 自带 salt
- 同一明文每次加密结果不同（防彩虹表）

### 4.4 SQL 注入防护

全部使用 MyBatis `#{}` 预编译参数，**禁止** `${}` 字符串拼接。

### 4.5 XSS 防护

- 服务端：Thymeleaf 默认 HTML 转义
- 客户端：输入校验

---

## 五、性能设计

### 5.1 数据库优化

- 主键索引：所有表 `id` 为主键
- 唯一索引：用户名、好友关系、群成员关系
- 联合索引：好友申请 `(to_user_id, status)`、群邀请 `(to_user_id, status)`、未读消息 `(to_id, is_read)`
- 合理冗余：`friend` 表冗余好友信息（昵称、头像），减少 JOIN

### 5.2 连接池配置

[Druid 配置](../src/main/resources/application.yml)：

```yaml
druid:
  initial-size: 5     # 初始连接数
  min-idle: 5         # 最小空闲
  max-active: 20      # 最大活跃
```

### 5.3 WebSocket 长连接

- 一次连接，多次推送
- 避免每条消息都建立 HTTP 连接的开销
- 断线自动重连（前端实现）

### 5.4 静态资源缓存

- Thymeleaf 模板：`cache: false`（开发模式）
- 静态资源：Spring Boot 默认带 hash 缓存策略

---

## 六、扩展性考虑

| 未来扩展点 | 当前预留 |
|-----------|---------|
| 群主转让 | `group_member.role` 字段已支持多级 |
| 消息已读回执 | `is_read` 字段已存在 |
| 多端登录 | Session 机制天然支持多设备 |
| 消息撤回 | 可在 `private_message` 表加 `revoked_time` 字段 |
| 文件断点续传 | 当前简单上传，未实现分片 |

---

## 七、参考文档

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/)
- [WebSocket + STOMP 指南](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html)
- [MyBatis 官方文档](https://mybatis.org/mybatis-3/zh/index.html)
