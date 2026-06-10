# 部署指南

> 文档版本：v1.0
> 最后更新：2026-06-10

---

## 一、部署架构

```
┌────────────────────┐         ┌──────────────────┐
│   用户浏览器        │  HTTP   │   Spring Boot    │
│  (Chrome/Edge/FF)  ├────────▶│   应用 (8080)     │
│                    │   WSS   │                  │
└────────────────────┘         └────────┬─────────┘
                                         │ JDBC
                                         ▼
                                ┌──────────────────┐
                                │   MySQL 8.0      │
                                │   (3306)         │
                                └──────────────────┘
```

---

## 二、部署方式

### 方式一：本地开发模式（推荐用于调试）

#### 步骤 1：启动 MySQL

```bash
# Windows
net start mysql

# Linux/macOS
sudo systemctl start mysql
```

#### 步骤 2：初始化数据库

```bash
mysql -u root -p < d:/C/online_chat/sql/chat_system.sql
```

#### 步骤 3：修改配置

打开 [src/main/resources/application.yml](../src/main/resources/application.yml)，修改：

```yaml
spring:
  datasource:
    username: root       # 你的 MySQL 用户名
    password: 1234       # 你的 MySQL 密码
```

#### 步骤 4：启动应用

```bash
cd d:/C/online_chat
mvn spring-boot:run
```

或直接用 IDE 运行 `ChatApplication.java`。

#### 步骤 5：访问

浏览器打开：http://localhost:8080

---

### 方式二：打包部署（推荐用于演示/作业提交）

#### 步骤 1：打包

```bash
cd d:/C/online_chat
mvn clean package -DskipTests
```

打包完成后，会在 `target/` 目录生成 `chat-system-1.0.0.jar`。

#### 步骤 2：复制到目标位置

将 jar 包复制到任意位置，例如 `d:/C/online_chat/dist/chat-system-1.0.0.jar`。

#### 步骤 3：运行

```bash
java -jar d:/C/online_chat/dist/chat-system-1.0.0.jar
```

#### 步骤 4：访问

浏览器打开：http://localhost:8080

---

### 方式三：服务器部署（Linux）

#### 步骤 1：上传文件

将 `chat-system-1.0.0.jar` 上传到服务器 `/opt/chat/`。

#### 步骤 2：配置 MySQL

```bash
# 安装 MySQL
sudo apt install mysql-server

# 启动
sudo systemctl start mysql

# 设置 root 密码
sudo mysql_secure_installation

# 创建数据库
mysql -u root -p
> CREATE DATABASE chat_system DEFAULT CHARACTER SET utf8mb4;
> source /opt/chat/chat_system.sql
```

#### 步骤 3：修改配置

服务器上的 application.yml 中：
- 数据库地址改为 `localhost:3306`（如果 MySQL 在同台机器）
- 账号密码改为服务器上的 MySQL 账号密码

或者用命令行参数覆盖：
```bash
java -jar chat-system-1.0.0.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/chat_system \
  --spring.datasource.username=root \
  --spring.datasource.password=YourPassword
```

#### 步骤 4：以后台进程运行

```bash
nohup java -jar chat-system-1.0.0.jar > /var/log/chat.log 2>&1 &
```

#### 步骤 5：配置开机自启（可选）

创建 systemd 服务 `/etc/systemd/system/chat.service`：

```ini
[Unit]
Description=Chat System
After=mysql.service

[Service]
Type=simple
User=root
ExecStart=/usr/bin/java -jar /opt/chat/chat-system-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动：
```bash
sudo systemctl enable chat
sudo systemctl start chat
sudo systemctl status chat
```

---

## 三、Druid 监控面板

本系统集成了 Druid 连接池，可访问监控面板：

- **URL**：http://localhost:8080/druid
- **登录账号**：`admin`
- **登录密码**：`admin`（可在 application.yml 中修改）

监控内容：
- SQL 执行统计
- 慢 SQL 记录
- 连接池状态
- URI 监控

**生产环境建议**：
- 修改默认账号密码
- 配置白名单 IP
- 关闭监控或限制访问

---

## 四、常见问题排查

### Q1：启动报错 "Communications link failure"

**原因**：MySQL 未启动或连接信息错误。

**解决**：
```bash
# 1. 检查 MySQL 是否启动
net start mysql       # Windows
sudo systemctl status mysql   # Linux

# 2. 验证账号密码
mysql -u root -p

# 3. 确认 application.yml 中账号密码正确
```

---

### Q2：启动报错 "Access denied for user 'root'@'localhost'"

**原因**：MySQL 8.0 默认使用 `caching_sha2_password` 认证，Java 驱动可能不兼容。

**解决**：
```sql
-- 修改用户认证方式
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '1234';
FLUSH PRIVILEGES;
```

---

### Q3：中文乱码

**原因**：数据库或连接字符集不匹配。

**解决**：
1. 确认数据库为 `utf8mb4`：
   ```sql
   SHOW VARIABLES LIKE 'character_set_database';
   ```
2. 确认 [application.yml](../src/main/resources/application.yml) 中 URL 包含：
   ```
   useUnicode=true&characterEncoding=utf-8
   ```

---

### Q4：WebSocket 连接失败

**现象**：浏览器控制台显示 `WebSocket connection to 'ws://...' failed`。

**原因**：
- 反向代理未配置 WebSocket 升级
- 防火墙阻止
- 浏览器/网络环境限制

**解决**：
- 客户端使用 SockJS 自动降级（已在 [chat.js](../src/main/resources/static/js/chat.js) 中实现）
- Nginx 配置：
  ```nginx
  location /ws {
      proxy_pass http://localhost:8080/ws;
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
  }
  ```

---

### Q5：上传文件失败 / 文件太大

**原因**：文件超过 10MB 限制。

**解决**：
修改 [application.yml](../src/main/resources/application.yml)：
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB        # 改为 50MB
      max-request-size: 50MB
```

---

### Q6：Maven 依赖下载慢

**解决**：配置阿里云镜像。编辑 `~/.m2/settings.xml`：

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

### Q7：端口 8080 被占用

**解决**：修改 [application.yml](../src/main/resources/application.yml)：
```yaml
server:
  port: 9090
```

或启动时指定：
```bash
java -jar chat-system-1.0.0.jar --server.port=9090
```

---

## 五、生产环境优化建议

### 5.1 安全

1. **修改 Druid 监控密码**：
   ```yaml
   spring.datasource.druid.stat-view-servlet.login-username: yourname
   spring.datasource.druid.stat-view-servlet.login-password: yourpass
   ```

2. **启用 HTTPS**：使用 Nginx 反向代理 + SSL 证书

3. **修改 MySQL 默认密码**

### 5.2 性能

1. **调整 Druid 连接池**：
   ```yaml
   spring.datasource.druid.initial-size: 10
   spring.datasource.druid.max-active: 50
   ```

2. **启用 Thymeleaf 模板缓存**：
   ```yaml
   spring.thymeleaf.cache: true
   ```

3. **使用 Nginx 做静态资源反向代理**（CSS/JS/图片）

### 5.3 日志

修改 [application.yml](../src/main/resources/application.yml)：
```yaml
logging:
  level:
    com.chat: info          # 生产环境调高日志级别
  file:
    name: /var/log/chat/chat.log
```

---

## 六、备份与恢复

### 6.1 数据库备份

```bash
# 备份
mysqldump -u root -p chat_system > chat_system_20260610.sql

# 恢复
mysql -u root -p chat_system < chat_system_20260610.sql
```

### 6.2 上传文件备份

上传的文件存储在 [application.yml](../src/main/resources/application.yml) 中配置的 `upload.path` 目录（默认 `d:/C/online_chat/upload/`），备份此目录即可。

---

## 七、版本升级

后续如果发布新版本：

1. 停止当前服务：`Ctrl+C` 或 `systemctl stop chat`
2. 备份数据（数据库 + 上传文件）
3. 替换 jar 包
4. 重新启动

数据库升级需手动执行额外的 SQL 迁移脚本（与新版本一同提供）。
