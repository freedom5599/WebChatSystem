# 小组分工方案

> 项目：在线聊天系统（Spring Boot + MyBatis + WebSocket）
> 小组成员：6 人

***

## 分工原则

根据课程要求，本次分工遵守以下核心原则：

1. **禁止单一角色**：不出现"只做数据库设计"、"只做前端页面"或"只做文档整理"的情况
2. **后端优先**：每位成员都有自己的后端模块（Controller + Service + Mapper + Entity）
3. **纵向划分**：按**功能模块**划分（而不是按"前端/后端/文档"横向划分），每人覆盖全栈
4. **工作量均衡**：每个模块规模相当，避免一人忙五人闲
5. **统一开发环境**：JDK 8、Maven 3.6+、MySQL 8.0+、IntelliJ IDEA

***

## 分工明细

### 👤  肖宇湘： 用户认证模块

| 类别     | 具体内容                                      |
| ------ | ----------------------------------------- |
| **后端** | `User` 实体、注册/登录 API、密码加密、Session 管理、登录拦截器 |
| **前端** | `login.html`、`register.html` 页面开发         |
| **文档** | 项目总体设计文档、架构图、部署说明                         |
| **统筹** | 协调组员分工、代码合并、版本管理、进度跟踪                     |

**涉及文件**：

- `entity/User.java`
- `service/UserService.java`
- `controller/UserController.java`
- `interceptor/LoginInterceptor.java`
- `templates/login.html`、`templates/register.html`
- `docs/REQUIREMENT.md`

***

### 👤 娄竣哲：好友系统模块

| 类别     | 具体内容                                                                                 |
| ------ | ------------------------------------------------------------------------------------ |
| **后端** | `Friend`、`FriendRequest`、`FriendGroup` 三个实体的完整 CRUD、好友申请处理（同意/拒绝）、好友分组管理（增删改）、好友移动分组 |
| **前端** | 侧边栏好友列表、好友分组展开/折叠、好友申请卡片 UI、好友分组管理弹窗                                                 |
| **文档** | 好友模块 API 文档、好友申请流程时序图                                                                |

**涉及文件**：

- `entity/Friend.java`、`entity/FriendRequest.java`、`entity/FriendGroup.java`
- `mapper/FriendMapper.java`、`mapper/FriendRequestMapper.java`、`mapper/FriendGroupMapper.java`
- `service/FriendService.java`
- `controller/FriendController.java`
- `dto/FriendRequestDTO.java`、`dto/FriendHandleDTO.java` 等
- `static/js/chat.js` 中好友相关函数（`loadFriendList`、`renderFriendRequests` 等）

***

### 👤 代志杰：私聊消息模块

| 类别     | 具体内容                                                        |
| ------ | ----------------------------------------------------------- |
| **后端** | `PrivateMessage` 实体、消息持久化、消息历史查询、未读消息处理、消息类型（文字/语音/图片/文件）分发 |
| **前端** | 私聊聊天窗口、消息气泡（自我/对方样式区分）、输入框、发送按钮、历史消息加载                      |
| **文档** | 消息模块设计文档、数据表结构说明、消息推送协议                                     |

**涉及文件**：

- `entity/PrivateMessage.java`
- `mapper/PrivateMessageMapper.java`
- `service/PrivateMessageService.java`
- `controller/MessageController.java`
- `static/js/chat.js` 中消息发送/接收相关函数
- `static/css/chat.css` 消息气泡样式

***

### 👤 付宝昊：群聊模块

| 类别     | 具体内容                                                                           |
| ------ | ------------------------------------------------------------------------------ |
| **后端** | `ChatGroup`、`GroupMember`、`GroupInvite` 三个实体的完整功能、建群/退群/解散、群主踢人、邀请入群（发送/接受/拒绝） |
| **前端** | 群列表 UI、群创建弹窗、群成员管理、群邀请列表、群聊窗口                                                  |
| **文档** | 群模块 API 文档、群权限设计说明                                                             |

**涉及文件**：

- `entity/ChatGroup.java`、`entity/GroupMember.java`、`entity/GroupInvite.java`
- `mapper/ChatGroupMapper.java`、`mapper/GroupMemberMapper.java`、`mapper/GroupInviteMapper.java`
- `service/GroupService.java`
- `controller/GroupController.java`
- `dto/GroupCreateDTO.java`、`dto/GroupJoinDTO.java` 等

***

### 👤 祝帅：WebSocket 实时通讯 + 文件传输

| 类别     | 具体内容                                                                      |
| ------ | ------------------------------------------------------------------------- |
| **后端** | WebSocket 配置（`WebSocketConfig`）、STOMP 消息分发、用户在线状态、消息广播、文件上传/下载 Controller |
| **前端** | 实时消息推送、好友上线/下线通知、新消息提示、语音/图片/文件消息渲染、聊天记录中图片点击放大                           |
| **文档** | WebSocket 协议说明、文件模块 API 文档、消息推送流程图                                        |

**涉及文件**：

- `config/WebSocketConfig.java`
- `controller/FileController.java`、`controller/ChatWebSocketController.java`
- `service/FileService.java`
- `static/js/chat.js` 中 WebSocket 连接、文件上传、消息分发相关函数
- `static/css/chat.css` 文件消息样式

***

### 👤 杨子琨：数据库 + 集成测试 + 总报告

| 类别      | 具体内容                                                        |
| ------- | ----------------------------------------------------------- |
| **数据库** | `chat_system.sql` 建表脚本、ER 图绘制、初始测试数据（测试用户、测试好友关系、测试群组）      |
| **测试**  | 编写测试用例、跑通全流程（注册 → 登录 → 加好友 → 私聊 → 建群 → 群聊 → 文件传输）、Bug 报告与回归 |
| **集成**  | Maven 项目打包（生成可运行 jar）、部署文档、演示环境准备                           |
| **文档**  | 用户手册、测试报告、演示 PPT、项目总结报告                                     |

**涉及文件**：

- `sql/chat_system.sql`
- `docs/API.md`、`docs/DEPLOY.md`
- 测试用例文档（自建）
- 演示 PPT（自建）
- `dist/chat-system-1.0.0.jar` 打包产物

***

## 分工校验表

| 校验项          | 状态                            |
| ------------ | ----------------------------- |
| ❌ "只做数据库" 角色 | 不存在（F 同时负责测试、集成、文档）           |
| ❌ "只做前端" 角色  | 不存在（每人都有后端模块）                 |
| ❌ "只做文档" 角色  | 不存在（文档伴随各自模块，不是独立工作）          |
| ✅ 每人有后端实质工作  | A/B/C/D/E 全部负责后端模块；F 负责建表 SQL |
| ✅ 工作量相对均衡    | 每个模块规模相当                      |
| ✅ 责任明确无重叠    | 按功能模块划分，互不冲突                  |

***

## 开发环境统一规范

| 项目          | 版本/工具                                   |
| ----------- | --------------------------------------- |
| JDK         | 1.8                                     |
| Maven       | 3.9.6（项目自带 `.tools/apache-maven-3.9.6`） |
| MySQL       | 8.0+                                    |
| IDE         | IntelliJ IDEA（推荐）或 Eclipse              |
| Spring Boot | 2.7.18                                  |
| 字符编码        | UTF-8                                   |
| 代码风格        | 阿里巴巴 Java 开发手册（简化版）                     |

***

## 协作规范

1. **Git 协作**：组长 A 创建仓库，每位成员一个分支，定期合并到 `main`
2. **接口先行**：后端接口先写好（可以用 Postman 测试），前端再对接
3. **每日同步**：每天组会同步进度，组长 A 记录到 \[项目进度表]
4. **代码审查**：合并前至少一名组员 review
5. **统一命名**：包名、类名、方法名严格遵循 Java 命名规范
6. **注释完整**：所有 public 方法必须有 Javadoc 注释

***

## 评分自查表（个人）

个人评分 = 项目评分（基础分） + 承担工作内容（主要分数来源） + 组长评分 + 报告心得（微调）

每位组员需要在最终报告的"心得体会"部分写明：

- 本人负责的模块及完成情况
- 在项目中遇到的难点及解决方案
- 团队协作中的收获

***

*最后更新：2026-06-10*
