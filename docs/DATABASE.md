# 数据库设计

> 文档版本：v1.0
> 最后更新：2026-06-10

---

## 一、数据库选型

- **类型**：MySQL 8.0+
- **字符集**：`utf8mb4`（支持 emoji 和生僻字）
- **排序规则**：`utf8mb4_unicode_ci`
- **存储引擎**：InnoDB（支持事务、外键）

---

## 二、ER 图

```
┌──────────┐         ┌────────────┐         ┌──────────┐
│   user   │◄────┐   │ friend_    │    ┌───│  friend  │
│          │     │   │  group     │    │   │          │
│ - id     │     │   │ - id       │    │   │ - id     │
│ - username│    │   │ - user_id  │────┘   │ - user_id│
│ - password│   │   │ - name     │        │ - friend_id│
│ - nickname│   │   │ - sort     │        │ - group_id│
│ - avatar │     │   └────────────┘        │ - remark │
│ - ...    │     │                         └──────────┘
└────┬─────┘     │                                ▲
     │           │                                │
     │           │    ┌──────────────┐            │
     │           └───►│  friend_     │────────────┘
     │                │  request     │
     │                │ - id         │
     │                │ - from_user_id
     │                │ - to_user_id │
     │                │ - message    │
     │                │ - status     │
     │                └──────────────┘
     │
     │  ┌────────────────┐
     ├─►│  private_      │
     │  │  message       │
     │  │ - id           │
     │  │ - from_id      │
     │  │ - to_id        │
     │  │ - content      │
     │  │ - msg_type     │
     │  │ - voice_url    │
     │  │ - is_read      │
     │  └────────────────┘
     │
     │  ┌────────────────┐         ┌──────────────┐
     ├─►│  chat_group    │◄────────┤ group_       │
     │  │ - id           │         │  member      │
     │  │ - name         │         │ - id         │
     │  │ - owner_id ────┼─────────┤ - group_id   │
     │  │ - description  │         │ - user_id    │
     │  └────────────────┘         │ - role       │
     │                             └──────┬───────┘
     │                                    │
     │  ┌────────────────┐                │
     ├─►│  group_invite  │                │
     │  │ - id           │                │
     │  │ - group_id     │                │
     │  │ - from_user_id │                │
     │  │ - to_user_id   │                │
     │  │ - message      │                │
     │  │ - status       │                │
     │  └────────────────┘                │
     │                                    │
     │  ┌────────────────┐                │
     └─►│  group_message │                │
        │ - id           │                │
        │ - group_id     │◄───────────────┘
        │ - from_id      │
        │ - content      │
        │ - msg_type     │
        │ - voice_url    │
        └────────────────┘
```

---

## 三、表结构详细说明

### 3.1 user（用户表）

存储用户基本信息。

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键，用户ID |
| `username` | VARCHAR(50) | 是 | - | 登录账号，唯一 |
| `password` | VARCHAR(100) | 是 | - | BCrypt 加密后的密码 |
| `nickname` | VARCHAR(50) | 否 | NULL | 昵称 |
| `avatar` | VARCHAR(255) | 否 | `/img/default-avatar.png` | 头像 URL |
| `signature` | VARCHAR(200) | 否 | `''` | 个性签名 |
| `status` | TINYINT | 否 | 0 | 在线状态：0 离线 / 1 在线 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 注册时间 |
| `update_time` | DATETIME | 否 | 自动更新 | 更新时间 |

**索引**：
- 主键：`id`
- 唯一索引：`uk_username(username)`

---

### 3.2 friend_group（好友分组表）

用户自定义的好友分组。

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT | 是 | - | 分组所属用户ID |
| `group_name` | VARCHAR(50) | 是 | - | 分组名称 |
| `sort_order` | INT | 否 | 0 | 排序序号（升序） |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- 主键：`id`
- 普通索引：`idx_user_id(user_id)`

---

### 3.3 friend（好友关系表）

记录用户之间的好友关系（双向，A 加 B 为好友 = 两条记录）。

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT | 是 | - | 用户ID |
| `friend_id` | BIGINT | 是 | - | 好友用户ID |
| `group_id` | BIGINT | 否 | NULL | 好友所属分组ID |
| `remark` | VARCHAR(50) | 否 | `''` | 好友备注名 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 添加时间 |

**索引**：
- 主键：`id`
- 唯一索引：`uk_user_friend(user_id, friend_id)` — 防止重复添加
- 普通索引：`idx_friend_id(friend_id)` — 反向查询

**设计要点**：
- A→B 和 B→A 是**两条独立记录**（A 备注"小李"，B 备注"老张"，互不干扰）
- 删除好友时也需双向删除

---

### 3.4 friend_request（好友申请表）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `from_user_id` | BIGINT | 是 | - | 申请人ID |
| `to_user_id` | BIGINT | 是 | - | 被申请人ID |
| `message` | VARCHAR(200) | 否 | `''` | 验证信息 |
| `status` | TINYINT | 否 | 0 | 状态：0 待处理 / 1 已同意 / 2 已拒绝 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 申请时间 |
| `handle_time` | DATETIME | 否 | NULL | 处理时间 |

**索引**：
- 主键：`id`
- 联合索引：`idx_to_user(to_user_id, status)` — 查询"我的待处理申请"
- 普通索引：`idx_from_user(from_user_id)` — 查询"我发起的申请"

**业务说明**：
- 处理后（同意/拒绝）记录**从数据库删除**，不保留历史

---

### 3.5 private_message（私聊消息表）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `from_id` | BIGINT | 是 | - | 发送者ID |
| `to_id` | BIGINT | 是 | - | 接收者ID |
| `content` | TEXT | 是 | - | 消息内容 |
| `msg_type` | TINYINT | 否 | 0 | 类型：0 文字 / 1 语音 / 2 图片 / 3 文件 |
| `voice_url` | VARCHAR(255) | 否 | NULL | 附件路径（非文字消息） |
| `is_read` | TINYINT | 否 | 0 | 是否已读：0 未读 / 1 已读 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 发送时间 |

**索引**：
- 主键：`id`
- 联合索引：`idx_from_to(from_id, to_id)` — 查询历史消息
- 联合索引：`idx_to_read(to_id, is_read)` — 查询未读消息数

---

### 3.6 chat_group（群组表）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `group_name` | VARCHAR(50) | 是 | - | 群名称 |
| `avatar` | VARCHAR(255) | 否 | `/img/default-group.png` | 群头像 |
| `owner_id` | BIGINT | 是 | - | 群主ID |
| `description` | VARCHAR(200) | 否 | `''` | 群简介 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- 主键：`id`
- 普通索引：`idx_owner(owner_id)`

---

### 3.7 group_member（群成员表）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `group_id` | BIGINT | 是 | - | 群组ID |
| `user_id` | BIGINT | 是 | - | 用户ID |
| `role` | TINYINT | 否 | 0 | 角色：0 普通成员 / 1 管理员 / 2 群主 |
| `join_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 加入时间 |

**索引**：
- 主键：`id`
- 唯一索引：`uk_group_user(group_id, user_id)` — 防止重复加入
- 普通索引：`idx_user_id(user_id)` — 查询"我加入的群"

---

### 3.8 group_invite（群邀请表）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `group_id` | BIGINT | 是 | - | 群组ID |
| `from_user_id` | BIGINT | 是 | - | 邀请人ID |
| `to_user_id` | BIGINT | 是 | - | 被邀请人ID |
| `message` | VARCHAR(200) | 否 | `''` | 邀请信息 |
| `status` | TINYINT | 否 | 0 | 状态：0 待处理 / 1 已同意 / 2 已拒绝 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 邀请时间 |
| `handle_time` | DATETIME | 否 | NULL | 处理时间 |

**索引**：
- 主键：`id`
- 联合索引：`idx_to_user(to_user_id, status)` — 查询"我的待处理邀请"
- 普通索引：`idx_group(group_id)` — 查询群的所有邀请

**业务说明**：
- 处理后记录**从数据库删除**

---

### 3.9 group_message（群聊消息表）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `id` | BIGINT | 是 | AUTO_INCREMENT | 主键 |
| `group_id` | BIGINT | 是 | - | 群组ID |
| `from_id` | BIGINT | 是 | - | 发送者ID |
| `content` | TEXT | 是 | - | 消息内容 |
| `msg_type` | TINYINT | 否 | 0 | 类型：0 文字 / 1 语音 / 2 图片 / 3 文件 |
| `voice_url` | VARCHAR(255) | 否 | NULL | 附件路径 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 发送时间 |

**索引**：
- 主键：`id`
- 普通索引：`idx_group_id(group_id)` — 查询群历史消息

---

## 四、初始化脚本

完整脚本见 [../sql/chat_system.sql](../sql/chat_system.sql)。执行该脚本会：

1. 创建 `chat_system` 库
2. 创建 9 张表
3. 插入 3 个测试用户（`zhangsan`、`lisi`、`wangwu`，密码均为 `123456`）
4. 插入测试好友关系、群组、消息

执行方式：
```bash
mysql -u root -p < sql/chat_system.sql
```

---

## 五、设计原则

1. **范式与反范式的平衡**：`friend` 表冗余了 `remark` 字段，避免频繁 JOIN
2. **索引最小化**：只为高频查询字段建索引，避免过多索引影响写入
3. **软删除 vs 硬删除**：
   - `user` / `friend` / `group_member` 采用硬删除
   - `friend_request` / `group_invite` 处理后立即硬删除（不留历史）
   - 消息保留（用于历史记录查询）
4. **字符集统一**：`utf8mb4` 防止 emoji 乱码
5. **时间字段**：`create_time` / `update_time` 由 MySQL 自动维护
