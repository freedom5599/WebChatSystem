# 在线聊天系统

> **Web程序设计课程期末大作业**
> 基于 Spring Boot + MyBatis + WebSocket + MySQL 实现的实时在线聊天系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-1.8-orange.svg)](https://www.java.com)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 一、项目简介

本项目是一个仿 QQ / 微信 的 Web 版即时聊天系统，支持**私聊**、**群聊**、**好友管理**、**群组管理**、**实时消息推送**、**文件传输**等核心功能。

### 1.1 项目目标

- 实践 Spring Boot 全栈开发
- 掌握 WebSocket 实时通信技术
- 熟悉 MyBatis 持久层框架
- 培养团队协作与版本管理能力

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| 🔐 用户系统 | 注册、登录、登出、密码 BCrypt 加密、会话管理 |
| 👥 好友管理 | 好友申请、同意/拒绝、好友分组、好友备注 |
| 💬 私聊 | 一对一实时聊天，支持文字/语音/图片/文件 |
| 🏘️ 群聊 | 创建群组、加入/退出、群主踢人、邀请入群 |
| 📡 实时推送 | WebSocket + STOMP 协议，毫秒级消息送达 |
| 📁 文件传输 | 语音、图片、文件上传下载，最大 10MB |
| 🎨 现代化 UI | 暖米白 + 深海青绿配色，圆角卡片，毛玻璃特效 |
| 📊 数据监控 | 集成 Druid 监控面板，实时查看 SQL 执行情况 |

---

## 二、技术栈

### 2.1 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | Web 应用框架 |
| Spring WebSocket | 5.3.x | 实时通信 |
| MyBatis | 2.3.2 | ORM 持久层 |
| MySQL | 8.0+ | 关系型数据库 |
| Druid | 1.2.20 | JDBC 连接池 + 监控 |
| Thymeleaf | 3.0.x | 服务端模板引擎 |
| Fastjson | 1.2.83 | JSON 处理 |
| BCrypt (Spring Security Crypto) | 5.7.11 | 密码加密 |
| Lombok | - | 简化 Java 代码 |
| Commons IO | 2.13.0 | 文件操作 |

### 2.2 前端

| 技术 | 说明 |
|------|------|
| HTML5 + CSS3 | 原生开发，无重型框架 |
| Vanilla JavaScript | 原生 JS，无 jQuery |
| SockJS + StompJS | WebSocket 客户端 |
| Google Fonts | Plus Jakarta Sans + Bricolage Grotesque |

### 2.3 开发工具

| 工具 | 版本/说明 |
|------|----------|
| JDK | 1.8 |
| Maven | 3.6+（项目自带 3.9.6） |
| IDE | IntelliJ IDEA（推荐） |
| 数据库客户端 | Navicat / DataGrip / DBeaver |

---

## 三、快速开始

### 3.1 环境准备

1. **安装 JDK 1.8** 并配置 `JAVA_HOME`
2. **安装 MySQL 8.0+** 并启动服务
3. **克隆项目**（或直接下载 ZIP）

### 3.2 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本（会自动建库 + 建表 + 插入测试数据）
source d:/C/online_chat/sql/chat_system.sql
```

脚本会创建 `chat_system` 数据库、9 张表、3 个测试用户（密码均为 `123456`）。

### 3.3 修改配置

打开 [src/main/resources/application.yml](../src/main/resources/application.yml)：

```yaml
spring:
  datasource:
    username: root      # 改为你的 MySQL 用户名
    password: 1234      # 改为你的 MySQL 密码
```

### 3.4 启动项目

#### 方式一：使用 Maven（推荐开发时使用）

```bash
cd d:/C/online_chat
mvn spring-boot:run
```

#### 方式二：使用打包好的 jar

```bash
java -jar d:/C/online_chat/dist/chat-system-1.0.0.jar
```

### 3.5 访问系统

打开浏览器访问：**http://localhost:8080**

测试账号：

| 用户名 | 密码 | 昵称 |
|--------|------|------|
| `zhangsan` | 123456 | 张三 |
| `lisi` | 123456 | 李四 |
| `wangwu` | 123456 | 王五 |

> 建议开两个浏览器（一个登录张三，一个登录李四）体验实时聊天效果。

---

## 四、项目结构

```
online_chat/
├── src/main/
│   ├── java/com/chat/
│   │   ├── ChatApplication.java          # 启动类
│   │   ├── common/                       # 通用类（Result、Session）
│   │   ├── config/                       # 配置类（Web、WebSocket、上传路径）
│   │   ├── controller/                   # 控制器层（User/Friend/Group/...）
│   │   ├── dto/                          # 数据传输对象
│   │   ├── entity/                       # 实体类
│   │   ├── interceptor/                  # 拦截器（登录校验）
│   │   ├── mapper/                       # MyBatis Mapper 接口
│   │   └── service/                      # 业务层
│   ├── resources/
│   │   ├── mapper/                       # MyBatis XML 映射文件
│   │   ├── static/                       # 静态资源（CSS、JS、图片）
│   │   ├── templates/                    # Thymeleaf 模板
│   │   └── application.yml               # Spring Boot 配置
│   └── sql/chat_system.sql               # 数据库初始化脚本
├── docs/                                  # 📚 项目文档（本目录）
├── dist/                                  # 打包产物
│   └── chat-system-1.0.0.jar
└── pom.xml                                # Maven 配置
```

---

## 五、文档导航

| 文档 | 内容 |
|------|------|
| [REQUIREMENT.md](REQUIREMENT.md) | 需求分析（功能/非功能需求、用例图） |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 系统架构（技术架构、模块划分、流程图） |
| [DATABASE.md](DATABASE.md) | 数据库设计（ER 图、表结构、索引说明） |
| [API.md](API.md) | API 接口文档（所有 RESTful 接口） |
| [WEBSOCKET.md](WEBSOCKET.md) | WebSocket 协议文档（STOMP 订阅/推送） |
| [DEPLOY.md](DEPLOY.md) | 部署指南（本地/服务器部署、问题排查） |
| [USER_MANUAL.md](USER_MANUAL.md) | 用户手册（功能使用说明） |
| [TEST_REPORT.md](TEST_REPORT.md) | 测试报告（测试用例、结果、覆盖率） |
| [TEAM_DIVISION.md](TEAM_DIVISION.md) | 小组分工（6 人任务分配） |

---

## 六、常见问题

### Q1：启动报错 "Communications link failure"
**A**：MySQL 未启动，或 [application.yml](../src/main/resources/application.yml) 中的账号密码错误。

### Q2：中文乱码
**A**：确认数据库字符集为 `utf8mb4`，[application.yml](../src/main/resources/application.yml) 中 URL 包含 `characterEncoding=utf-8`。

### Q3：WebSocket 连接失败
**A**：检查浏览器控制台，确认 SockJS 降级方案是否生效（部分企业内网会拦截 WebSocket）。

### Q4：上传文件失败
**A**：检查 [application.yml](../src/main/resources/application.yml) 中 `multipart.max-file-size` 配置（默认 10MB）。

更多问题排查请参考 [DEPLOY.md](DEPLOY.md) 第四节。

---

## 七、版本信息

- **当前版本**：v1.0.0
- **最后更新**：2026-06-10
- **开发者**：6 人小组

---

## 八、许可证

注意：本项目仅用于课程学习，请勿用于商业用途。
