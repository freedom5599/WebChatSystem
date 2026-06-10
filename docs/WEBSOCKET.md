# WebSocket 协议文档

> 文档版本：v1.0
> 最后更新：2026-06-10

---

## 一、概述

本系统使用 **WebSocket + STOMP** 协议实现实时通信。WebSocket 提供全双工通信能力，STOMP 在 WebSocket 之上定义了消息格式（类似 HTTP）。

### 1.1 选型说明

| 特性 | WebSocket | STOMP |
|------|----------|-------|
| 作用 | 传输层 | 消息层（基于 WS） |
| 协议 | RFC 6455 | 简单文本协议 |
| 浏览器支持 | 现代浏览器原生 | 通过 SockJS+StompJS |

### 1.2 依赖

- **服务端**：`spring-boot-starter-websocket`（已包含 STOMP）
- **客户端**：`sockjs.min.js` + `stomp.min.js`

---

## 二、连接建立

### 2.1 端点

- **SockJS 端点**：`/ws`
- **STOMP 端点**：`/ws` 升级为 WebSocket

### 2.2 连接流程

```javascript
// 1. 创建 SockJS 连接
var socket = new SockJS('/ws');

// 2. 创建 STOMP 客户端
var stompClient = Stomp.over(socket);

// 3. 连接
stompClient.connect({}, function(frame) {
    console.log('连接成功: ' + frame);
    // 4. 订阅 topic
    stompClient.subscribe('/topic/private/' + currentUser.id, onMessage);
    // ... 更多订阅
}, function(error) {
    console.log('连接失败: ' + error);
});
```

### 2.3 鉴权

WebSocket 握手时携带 `JSESSIONID` Cookie，服务端的 [WebSocketConfig](../src/main/java/com/chat/config/WebSocketConfig.java) 配置了拦截器 [HandshakeInterceptor](../src/main/java/com/chat/config/WebSocketConfig.java) 校验 Session。未登录的连接会被拒绝。

---

## 三、订阅地址

订阅（接收消息）使用 `/topic/...` 前缀。客户端必须在连接后立即订阅。

### 3.1 私聊消息

- **订阅地址**：`/topic/private/{userId}`
- **触发**：任何人向你（userId）发送私聊消息时
- **消息体**：

```json
{
  "id": 1,
  "fromId": 2,
  "toId": 1,
  "content": "你好",
  "msgType": 0,
  "voiceUrl": null,
  "createTime": "2026-06-10 10:00:00"
}
```

| 字段 | 说明 |
|------|------|
| `id` | 消息ID |
| `fromId` | 发送者ID |
| `toId` | 接收者ID（即当前用户） |
| `content` | 消息内容（文字时是文本，图片/文件时是URL） |
| `msgType` | 0 文字 / 1 语音 / 2 图片 / 3 文件 |
| `voiceUrl` | 语音/图片/文件的URL（文字时为 null） |
| `createTime` | 发送时间 |

---

### 3.2 用户在线状态

- **订阅地址**：`/topic/user/status`
- **触发**：任何用户登录/登出时
- **消息体**：

```json
{ "userId": 1, "status": 1 }
```

| 字段 | 取值 |
|------|------|
| `status` | 0 离线 / 1 在线 |

---

### 3.3 系统通知

- **订阅地址**：`/topic/notification/{userId}`
- **触发**：与当前用户相关的通知事件（好友申请、群邀请等）
- **消息体**（按 type 区分）：

#### 3.3.1 好友申请 `type: "friend_request"`

```json
{
  "type": "friend_request",
  "requestId": 1,
  "fromUserId": 2,
  "fromUsername": "lisi",
  "fromNickname": "李四",
  "fromAvatar": "/img/default-avatar.png",
  "message": "我是李四，想加你为好友"
}
```

#### 3.3.2 好友申请结果 `type: "friend_request_result"`

```json
{
  "type": "friend_request_result",
  "requestId": 1,
  "status": 1,
  "fromUserId": 1,
  "toUserId": 2
}
```

| 字段 | 取值 |
|------|------|
| `status` | 1 同意 / 2 拒绝 |

#### 3.3.3 群邀请 `type: "group_invite"`

```json
{
  "type": "group_invite",
  "inviteId": 1,
  "groupId": 1,
  "groupName": "技术交流群",
  "fromUserId": 1
}
```

---

## 四、消息发送

发送（出站消息）使用 `/app/...` 前缀。但本系统**私聊消息**主要通过 REST API + 服务端推送实现，**不需要**客户端用 STOMP SEND。

如果要扩展为客户端直接发送私聊，可参考如下设计：

### 4.1 客户端发送私聊

```javascript
stompClient.send("/app/private", {}, JSON.stringify({
    toId: 2,
    content: "你好",
    msgType: 0
}));
```

服务端 [ChatWebSocketController](../src/main/java/com/chat/controller/ChatWebSocketController.java) 接收后：
1. 写入数据库
2. 推送到 `/topic/private/{toId}`

---

## 五、群消息

群消息的发送和接收是**广播**模式：

- **发送**：通过 WebSocket 发送到 `/app/group` 或 REST API
- **接收**：所有群成员订阅 `/topic/group/{groupId}`

**消息体**：

```json
{
  "id": 1,
  "groupId": 1,
  "fromId": 2,
  "fromNickname": "李四",
  "fromAvatar": "/img/default-avatar.png",
  "content": "大家好",
  "msgType": 0,
  "createTime": "2026-06-10 10:00:00"
}
```

---

## 六、断线重连

前端代码 [chat.js](../src/main/resources/static/js/chat.js) 实现了自动重连：

```javascript
reconnectDelay = 5000;  // 5秒后重连
stompClient.connect({}, onConnect, function(error) {
    console.log('连接失败: ' + error);
    setTimeout(connectWebSocket, reconnectDelay);
});
```

**重连策略**：
- 连接断开后等待 5 秒
- 重新建立连接
- 重新订阅所有 topic

---

## 七、消息可靠性

### 7.1 消息丢失场景

| 场景 | 处理 |
|------|------|
| 发送时对方不在线 | 消息存入数据库，对方上线后通过 `getPrivateHistory` 拉取 |
| 网络抖动导致消息丢失 | 客户端重连后会自动重新订阅，但中间的消息可能丢失（建议加入消息确认机制） |
| 服务端崩溃 | 消息已写入数据库，恢复后历史消息仍可查询 |

### 7.2 消息顺序

- 同一对用户的私聊消息通过 `id` 自增保证顺序
- 群消息同理

---

## 八、性能考虑

| 指标 | 说明 |
|------|------|
| 并发连接 | 单机支持 5000+ WebSocket 连接（基于 Netty） |
| 消息延迟 | 局域网 < 100ms |
| 心跳 | STOMP 默认支持（`heart-beat: 10000,10000`） |

---

## 九、安全考虑

| 风险 | 缓解 |
|------|------|
| 未授权订阅 | HandshakeInterceptor 校验 Session |
| 消息伪造 | 发送者 ID 从 Session 取，不信任客户端 |
| XSS | 消息内容前端转义 |

---

## 十、调试技巧

### 10.1 浏览器查看

Chrome DevTools → Network → WS：

![WebSocket Debug](https://via.placeholder.com/600x300?text=WebSocket+Debug)

可以查看所有 STOMP 帧的收发。

### 10.2 服务端日志

[application.yml](../src/main/resources/application.yml) 中开启 DEBUG 日志：

```yaml
logging:
  level:
    org.springframework.web.socket: DEBUG
    com.chat: DEBUG
```

### 10.3 测试工具

- **wscat**：`npm install -g wscat`，然后 `wscat -c ws://localhost:8080/ws`
- **Postman**：支持 WebSocket 测试
