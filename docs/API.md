# API 接口文档

> 文档版本：v1.0
> 最后更新：2026-06-10

---

## 通用说明

### 1. 基础信息

- **Base URL**：`http://localhost:8080`
- **请求/响应格式**：`application/json; charset=utf-8`
- **字符编码**：UTF-8

### 2. 统一返回格式

```typescript
interface Result<T> {
    code: number;       // 状态码：200 成功，其他失败
    msg: string;        // 提示信息
    data: T | null;     // 返回数据
}
```

**示例**：
```json
{
  "code": 200,
  "msg": "success",
  "data": { "id": 1, "username": "zhangsan" }
}
```

### 3. 状态码

| 状态码 | 含义 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录 / 会话失效 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

### 4. 鉴权

- 除 `/api/user/register`、`/api/user/login` 外，**所有 `/api/**` 接口需要登录**
- 登录后服务端通过 `JSESSIONID` Cookie 识别用户
- 未登录返回 401，前端需跳转登录页

### 5. 错误响应示例

```json
{
  "code": 401,
  "msg": "未登录",
  "data": null
}
```

---

## 一、用户模块 `/api/user`

### 1.1 用户注册

- **URL**：`POST /api/user/register`
- **是否需要登录**：否
- **请求体**：

```json
{
  "username": "newuser",
  "nickname": "新用户",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 用户名，3-50位 |
| `nickname` | string | 否 | 昵称 |
| `password` | string | 是 | 密码，6-20位 |

- **响应**：

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": null
}
```

**错误情况**：
- 用户名已存在
- 用户名/密码长度不合法

---

### 1.2 用户登录

- **URL**：`POST /api/user/login`
- **是否需要登录**：否
- **请求体**：

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "/img/default-avatar.png",
    "signature": "今天天气真好~",
    "status": 1
  }
}
```

**注意**：
- 登录成功后，浏览器会收到 `JSESSIONID` Cookie，后续请求需自动携带
- 登录成功后服务端会通过 WebSocket 广播该用户的在线状态

---

### 1.3 用户登出

- **URL**：`POST /api/user/logout`
- **是否需要登录**：是
- **请求体**：无
- **响应**：

```json
{ "code": 200, "msg": "success", "data": null }
```

---

### 1.4 获取当前用户信息

- **URL**：`GET /api/user/info`
- **是否需要登录**：是
- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "/img/default-avatar.png",
    "signature": "今天天气真好~"
  }
}
```

---

### 1.5 根据 ID 获取用户信息

- **URL**：`GET /api/user/info/{id}`
- **是否需要登录**：否
- **路径参数**：`id` 用户ID
- **响应**：同 1.4

---

### 1.6 按用户名搜索用户

- **URL**：`GET /api/user/search?username={username}`
- **是否需要登录**：是
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 用户名（精确匹配） |

- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 2,
    "username": "lisi",
    "nickname": "李四",
    "avatar": "/img/default-avatar.png",
    "signature": "学习使我快乐"
  }
}
```

---

### 1.7 修改个人信息

- **URL**：`POST /api/user/update`
- **是否需要登录**：是
- **请求体**：

```json
{
  "nickname": "新昵称",
  "avatar": "/img/avatar-1.png",
  "signature": "新签名"
}
```

- **响应**：返回更新后的用户对象（密码字段为 null）

---

## 二、好友模块 `/api/friend`

### 2.1 获取好友列表

- **URL**：`GET /api/friend/list`
- **是否需要登录**：是
- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "friendId": 2,
      "groupId": 1,
      "groupName": "我的好友",
      "remark": "小李",
      "friendUsername": "lisi",
      "friendNickname": "李四",
      "friendAvatar": "/img/default-avatar.png",
      "friendSignature": "学习使我快乐",
      "online": false
    }
  ]
}
```

---

### 2.2 发送好友申请

- **URL**：`POST /api/friend/request`
- **是否需要登录**：是
- **请求体**：

```json
{
  "toUserId": 2,
  "message": "我是张三，想加你为好友"
}
```

- **响应**：

```json
{ "code": 200, "msg": "好友申请已发送", "data": null }
```

**业务规则**：
- 不能添加自己为好友
- 已是好友不能重复添加
- 目标用户必须存在

**副作用**：
- 向目标用户推送 WebSocket 通知 `/topic/notification/{toUserId}`

```json
{
  "type": "friend_request",
  "requestId": 1,
  "fromUserId": 1,
  "fromUsername": "zhangsan",
  "fromNickname": "张三",
  "fromAvatar": "/img/default-avatar.png",
  "message": "我是张三，想加你为好友"
}
```

---

### 2.3 处理好友申请（同意/拒绝）

- **URL**：`POST /api/friend/handle`
- **是否需要登录**：是
- **请求体**：

```json
{
  "requestId": 1,
  "status": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `requestId` | long | 是 | 申请记录ID |
| `status` | int | 是 | 1 同意 / 2 拒绝 |

- **响应**：

```json
{ "code": 200, "msg": "已添加为好友", "data": null }
```

**业务逻辑**：
- 同意：双向插入 friend 记录 + 删除 friend_request 记录 + 通知申请人
- 拒绝：删除 friend_request 记录 + 通知申请人

**副作用**（仅同意/拒绝时）：
- 向申请人推送 `/topic/notification/{fromUserId}`

```json
{
  "type": "friend_request_result",
  "requestId": 1,
  "status": 1,
  "fromUserId": 1,
  "toUserId": 2
}
```

---

### 2.4 获取待处理好友申请列表

- **URL**：`GET /api/friend/requests`
- **是否需要登录**：是
- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "fromUserId": 1,
      "fromUsername": "zhangsan",
      "fromNickname": "张三",
      "fromAvatar": "/img/default-avatar.png",
      "message": "我是张三，想加你为好友",
      "status": 0,
      "createTime": "2026-06-10 10:00:00"
    }
  ]
}
```

---

### 2.5 删除好友

- **URL**：`DELETE /api/friend/delete?friendId={id}`
- **是否需要登录**：是
- **查询参数**：`friendId` 好友用户ID
- **响应**：

```json
{ "code": 200, "msg": "好友已删除", "data": null }
```

**业务逻辑**：双向删除 friend 记录。

---

### 2.6 移动好友到其他分组

- **URL**：`POST /api/friend/move`
- **是否需要登录**：是
- **请求体**：

```json
{
  "friendId": 2,
  "groupId": 2
}
```

---

### 2.7 获取好友分组列表

- **URL**：`GET /api/friend/groups`
- **是否需要登录**：是
- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    { "id": 1, "userId": 1, "groupName": "我的好友", "sortOrder": 1 },
    { "id": 2, "userId": 1, "groupName": "同学", "sortOrder": 2 }
  ]
}
```

---

### 2.8 创建好友分组

- **URL**：`POST /api/friend/group/add`
- **是否需要登录**：是
- **请求体**：

```json
{ "groupName": "家人" }
```

---

### 2.9 删除好友分组

- **URL**：`DELETE /api/friend/group/delete?groupId={id}`
- **是否需要登录**：是
- **说明**：该分组下的好友会迁移到其他分组（默认第一个剩余分组）

---

## 三、群组模块 `/api/group`

### 3.1 创建群组

- **URL**：`POST /api/group/create`
- **是否需要登录**：是
- **请求体**：

```json
{ "groupName": "技术交流群", "description": "一起讨论技术" }
```

- **响应**：

```json
{ "code": 200, "msg": "success", "data": { "id": 1, "groupName": "技术交流群", ... } }
```

---

### 3.2 解散群组（群主）

- **URL**：`POST /api/group/dissolve?groupId={id}`
- **是否需要登录**：是（仅群主）

---

### 3.3 退出群组

- **URL**：`POST /api/group/leave?groupId={id}`
- **是否需要登录**：是（群主不能退出，需先解散）

---

### 3.4 通过群ID加入群组

- **URL**：`POST /api/group/join?groupId={id}`
- **是否需要登录**：是
- **说明**：直接加入，不需要审批

---

### 3.5 邀请好友入群

- **URL**：`POST /api/group/invite`
- **是否需要登录**：是
- **请求体**：

```json
{ "groupId": 1, "inviteUserId": 3 }
```

**副作用**：
- 向被邀请人推送 `/topic/notification/{inviteUserId}`

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

### 3.6 接受群邀请

- **URL**：`POST /api/group/invite/accept?inviteId={id}`
- **是否需要登录**：是
- **业务逻辑**：加入群成员 + 删除邀请记录

---

### 3.7 拒绝群邀请

- **URL**：`POST /api/group/invite/reject?inviteId={id}`
- **是否需要登录**：是
- **业务逻辑**：删除邀请记录

---

### 3.8 获取我的群邀请列表

- **URL**：`GET /api/group/invites`
- **是否需要登录**：是
- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "groupId": 1,
      "groupName": "技术交流群",
      "fromUserId": 1,
      "fromNickname": "张三",
      "message": "邀请你加入群聊【技术交流群】",
      "status": 0,
      "createTime": "2026-06-10 10:00:00"
    }
  ]
}
```

---

### 3.9 获取我的群列表

- **URL**：`GET /api/group/my`
- **是否需要登录**：是

---

### 3.10 获取群成员列表

- **URL**：`GET /api/group/members?groupId={id}`
- **是否需要登录**：是

---

### 3.11 踢出群成员（群主）

- **URL**：`POST /api/group/kick`
- **请求体**：

```json
{ "groupId": 1, "kickUserId": 3 }
```

---

## 四、私聊消息 `/api/message`

### 4.1 获取私聊历史消息

- **URL**：`GET /api/message/private/history?friendId={id}`
- **是否需要登录**：是
- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "fromId": 1,
      "toId": 2,
      "content": "你好",
      "msgType": 0,
      "isRead": 1,
      "createTime": "2026-06-10 10:00:00"
    }
  ]
}
```

---

### 4.2 标记消息已读

- **URL**：`POST /api/message/private/read?friendId={id}`
- **是否需要登录**：是

---

### 4.3 分页获取历史消息

- **URL**：`GET /api/message/private/history/paged`
- **查询参数**：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `friendId` | long | - | 好友ID |
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页条数 |

---

### 4.4 导出聊天记录

- **URL**：`GET /api/message/private/export?friendId={id}`
- **响应**：返回聊天记录的文本内容（前端展示/下载）

---

## 五、文件模块 `/api/file`

### 5.1 上传文件

- **URL**：`POST /api/file/upload`
- **Content-Type**：`multipart/form-data`
- **请求体**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file | 是 | 文件（图片/语音/文件） |

- **限制**：单文件最大 10MB
- **响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": "/upload/abc123.jpg"
}
```

返回的 `data` 是文件的访问 URL，前端可直接使用 `<img src="...">` 显示，或在消息中引用。

---

## 六、群消息

群消息通过 WebSocket 发送（不是 REST API），参见 [WEBSOCKET.md](WEBSOCKET.md)。

历史群消息查询接口：
- **URL**：`GET /api/group/message/history?groupId={id}`

---

## 七、调试工具

推荐使用 **Postman** 或 **Apifox** 调试接口。

**调试步骤**：
1. 调用 `/api/user/login` 登录，浏览器会保存 `JSESSIONID` Cookie
2. 在 Postman 中设置 `Cookie` 自动携带
3. 后续请求会自动包含 Cookie，服务端可识别用户

**Curl 示例**：
```bash
# 登录（保存 Cookie）
curl -c cookies.txt -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'

# 后续请求携带 Cookie
curl -b cookies.txt http://localhost:8080/api/user/info
```
