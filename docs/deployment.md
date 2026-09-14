# 部署与运行指南

## 1. 适用范围

本文档以当前仓库的构建配置、应用配置和启动脚本为准，说明以下三种运行方式：

1. Windows 本机一键演示：使用 `scripts/start-demo.ps1` 启动隔离的 MySQL、后端和前端。
2. 本地开发与联调：使用已有 MySQL，分别启动 Spring Boot 和 Vite。
3. 构建产物部署：运行后端 JAR，并由 Web 服务器托管前端 `dist/`、处理 SPA 回退和 `/api` 反向代理。

一键演示的页面操作和建议演示顺序见[本地演示指南](demo.md)。接口路径和鉴权约定见[接口契约](../api-contract.md)。

> `scripts/start-demo.ps1` 只用于 Windows 本机演示。它创建空密码、仅监听回环地址的隔离 MySQL，不能作为生产部署方案。

## 2. 环境要求

| 组件 | 项目要求 | 2026-09-14 验证版本 | 说明 |
| --- | --- | --- | --- |
| 操作系统 | 一键脚本要求 Windows | Windows 11 amd64 | 手动部署可按目标系统调整命令 |
| PowerShell | 可运行 `powershell.exe` | Windows PowerShell 5.1 | 一键演示入口 |
| JDK | 21 | Eclipse Temurin 21.0.12.1 LTS | `JAVA_HOME` 指向 JDK 21 |
| Maven | 使用仓库 Wrapper | Maven 3.9.16 | 无需单独安装；首次下载需要网络 |
| Node.js | `^20.19.0` 或 `>=22.12.0` | 24.20.0 | 要求来自锁定的 Vite 8.2.2 |
| npm | 随兼容 Node.js 安装 | 11.19.0 | Windows PowerShell 中使用 `npm.cmd` |
| Python | Python 3 | 3.13.15 | 演示数据与自动检查仅使用标准库 |
| MySQL | 8.4 | 8.4.9 Community Server | 一键脚本要求客户端和服务端命令在 PATH 中 |

首次使用 Maven Wrapper、执行 `npm ci` 或下载尚未缓存的依赖时需要访问对应的软件包仓库。

在仓库根目录检查命令是否可用：

```powershell
Get-Command java.exe,node.exe,npm.cmd,python.exe,mysql.exe,mysqld.exe,mysqladmin.exe
java.exe --version
node.exe --version
npm.cmd --version
python.exe --version
.\backend\mvnw.cmd --version
mysqld.exe --version
```

## 3. 获取仓库

```powershell
git clone https://github.com/zhipeng-yu/elm-lite-platform.git
Set-Location .\elm-lite-platform
git status --short
```

所有后续命令若未特别说明，均从仓库根目录开始执行。部署前应确认所需分支或提交已经检出，并保持工作区没有意外修改。

## 4. Windows 一键演示

### 4.1 启动前检查

脚本需要本机端口 `13317`、`18081`、`5180` 均未被占用：

```powershell
Get-NetTCPConnection `
    -LocalPort 13317,18081,5180 `
    -State Listen `
    -ErrorAction SilentlyContinue |
    Select-Object LocalAddress,LocalPort,OwningProcess
```

没有输出表示这些端口当前没有监听进程。若有输出，应先确认并正常停止已有演示进程，不要直接结束不明进程。

### 4.2 启动与访问

```powershell
powershell.exe `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File .\scripts\start-demo.ps1
```

脚本会依次执行：

1. 检查三个演示端口。
2. 通过 `backend/mvnw.cmd verify` 运行后端测试并构建 JAR。
3. 前端依赖不存在时执行 `npm.cmd ci`。
4. 在仓库根目录的 `.local-demo/mysql/` 初始化隔离 MySQL；若检测到旧版 `backend/target/local-demo/`，脚本会在安全检查后整体迁移。
5. 首次运行时执行 V1、V2 和基础种子 SQL，再按版本号补齐 V3 及之后的迁移。
6. 运行 `scripts/seed-demo.py`，保证至少 30 家店铺且每店至少 6 件商品，不重置已有库存。
7. 为当前进程随机生成 JWT 密钥，启动后端和关闭 mock 的前端。
8. 通过前端代理请求 `/api/v1/shops`，成功后输出 `Demo ready`。

看到以下提示后再打开页面：

```text
Demo ready: http://127.0.0.1:5180
```

浏览器入口为 <http://127.0.0.1:5180/home>。启动终端必须保持运行；不要在演示过程中按 Enter。

### 4.3 演示端口与数据

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 隔离 MySQL | `127.0.0.1:13317` | 空密码本地 root，仅用于虚构演示数据 |
| Spring Boot | `127.0.0.1:18081` | 仅供本机前端代理访问 |
| Vite | `127.0.0.1:5180` | 浏览器统一入口 |

演示数据和日志保存在 Git 忽略的仓库根目录 `.local-demo/`，不会被 Maven `clean` 删除，再次启动时继续使用。脚本每次启动都会重新生成 JWT 密钥，因此重启后需要重新登录。

### 4.4 日志与停止

| 文件 | 内容 |
| --- | --- |
| `verify.log` | Maven `verify` 输出 |
| `mysql-init.log` | MySQL 数据目录初始化输出 |
| `mysql.log` / `mysql-error.log` | MySQL 运行日志 |
| `backend.log` / `backend-error.log` | 后端标准输出与错误输出 |
| `frontend.log` / `frontend-error.log` | 前端标准输出与错误输出 |

演示完成后回到启动终端按 Enter。脚本会停止前端、后端和隔离 MySQL。不要通过删除 `.local-demo/` 来停止服务。

## 5. 手动初始化数据库

本节用于已有 MySQL 的本地开发或受控部署，不使用一键演示的隔离数据库。

### 5.1 创建数据库

使用具有建库和建表权限的账号连接 MySQL；`--password` 会交互式提示输入，不要把密码直接写在命令行中：

```powershell
$zqDbUser = Read-Host 'MySQL 用户名'
mysql.exe `
    --protocol=TCP `
    --host=127.0.0.1 `
    --port=3306 `
    "--user=$zqDbUser" `
    --password `
    --default-character-set=utf8mb4
```

在 MySQL 客户端中执行：

```sql
CREATE DATABASE IF NOT EXISTS elm_lite CHARACTER SET utf8mb4;
USE elm_lite;
```

### 5.2 执行迁移

必须在全新数据库中按版本号依次执行全部迁移。把下列 `<仓库绝对路径>` 替换为本机路径，并在 MySQL 的 `SOURCE` 路径中使用正斜杠：

```sql
SOURCE <仓库绝对路径>/database/migration/V1__create_initial_schema.sql;
SOURCE <仓库绝对路径>/database/migration/V2__add_username_to_users.sql;
SOURCE <仓库绝对路径>/database/migration/V3__add_product_detail_images.sql;
SOURCE <仓库绝对路径>/database/migration/V4__create_admin_account.sql;
SOURCE <仓库绝对路径>/database/migration/V5__create_coupon_tables.sql;
SOURCE <仓库绝对路径>/database/migration/V6__create_rider_and_order_assignment.sql;
SOURCE <仓库绝对路径>/database/migration/V7__add_coupon_fields_to_orders.sql;
```

开发或演示环境可在全部迁移成功后导入虚构种子数据：

```sql
SOURCE <仓库绝对路径>/database/init/V1__seed_data.sql;
```

生产环境默认不导入演示数据。已经应用的迁移不得修改或重复执行；后续结构变更必须新增更高版本迁移，并在执行前完成备份和回滚评估。

## 6. 后端配置与部署

### 6.1 环境变量

| 变量 | 必需 | 默认值 | 用途 |
| --- | --- | --- | --- |
| `DB_URL` | 建议显式设置 | `jdbc:mysql://localhost:3306/elm_lite?...` | JDBC 连接地址 |
| `DB_USERNAME` | 是 | 无 | 数据库账号 |
| `DB_PASSWORD` | 是 | 无 | 数据库密码 |
| `JWT_SECRET` | 是 | 无 | JWT 签名密钥，至少使用 32 字节随机数据 |
| `SERVER_PORT` | 否 | `8080` | 后端监听端口 |
| `ADMIN_USERNAME` | 否 | 空 | 首次受控创建管理员时使用 |
| `ADMIN_PASSWORD` | 否 | 空 | 首次受控创建管理员时使用 |

本地 PowerShell 示例：

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/elm_lite?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = '<数据库用户名>'
$env:DB_PASSWORD = '<数据库密码>'
$env:SERVER_PORT = '8080'
```

在当前进程内生成不会输出到终端的随机 JWT 密钥：

```powershell
$zqSecretBytes = New-Object byte[] 48
$zqRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
$zqRandom.GetBytes($zqSecretBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($zqSecretBytes)
$zqRandom.Dispose()
```

示例占位符必须替换为当前环境的真实配置，但不得把真实值写入仓库、截图、日志或 PR。生产环境应通过操作系统服务配置或密钥管理系统注入，而不是保存在脚本或配置文件中。

如需首次初始化管理员，可只在受控启动时临时设置：

```powershell
$env:ADMIN_USERNAME = '<管理员用户名>'
$env:ADMIN_PASSWORD = '<管理员强密码>'
```

确认管理员已创建且密码保存为摘要后，清除当前终端中的初始化凭据：

```powershell
Remove-Item Env:ADMIN_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

### 6.2 测试与构建

```powershell
Push-Location .\backend
try {
    .\mvnw.cmd verify
} finally {
    Pop-Location
}
```

测试使用 H2 的 MySQL 兼容模式，不依赖本机 MySQL。覆盖率报告生成在 `backend/target/site/jacoco/index.html`，可执行 JAR 生成在：

```text
backend/target/elm-lite-platform-0.0.1-SNAPSHOT.jar
```

### 6.3 启动与验证

开发时可在 `backend/` 中运行：

```powershell
.\mvnw.cmd spring-boot:run
```

运行构建产物：

```powershell
java.exe -jar .\backend\target\elm-lite-platform-0.0.1-SNAPSHOT.jar
```

后端启动后，在另一个终端进行真实 HTTP 检查：

```powershell
$zqResponse = Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/shops'
$zqResponse.code
```

预期返回 `0`。H2 自动化测试不能替代这一步真实 MySQL 联调。

## 7. 前端配置与部署

### 7.1 安装、测试和构建

```powershell
Push-Location .\front-end
try {
    npm.cmd ci
    node.exe --test tests/*.test.js
    $env:VITE_USE_MOCK = 'false'
    npm.cmd run build
} finally {
    Pop-Location
}
```

生产构建产物位于 `front-end/dist/`。部署真实系统时必须在构建前保持 `VITE_USE_MOCK=false`。

### 7.2 本地开发服务器

真实联调前先启动后端，然后执行：

```powershell
Set-Location .\front-end
$env:VITE_USE_MOCK = 'false'
$env:API_PROXY_TARGET = 'http://127.0.0.1:8080'
npm.cmd run dev
```

默认访问 <http://127.0.0.1:5173/home>。`API_PROXY_TARGET` 只控制 Vite 开发服务器的 `/api` 代理；它不会为已经构建的静态文件配置生产代理。

### 7.3 静态文件服务器要求

仓库当前没有提交 Docker、Compose 或 Nginx 配置。部署 `front-end/dist/` 时，所选 Web 服务器必须由部署人员配置：

1. 通过 HTTPS 提供静态文件。
2. 将 `/api` 请求反向代理到 Spring Boot。
3. 前端使用 `createWebHistory()`，未知的非文件路径必须回退到 `index.html`，否则直接刷新路由会返回 404。
4. 不把后端数据库端口暴露给浏览器或公网。

`npm.cmd run preview` 只用于预览构建产物，不作为生产 Web 服务器。

## 8. 端口关系

| 模式 | MySQL | 后端 | 前端 |
| --- | --- | --- | --- |
| 手动本地开发默认值 | `3306` | `8080` | `5173` |
| 一键本机演示 | `13317` | `18081` | `5180` |
| 生产部署 | 内网受限端口 | 内网应用端口 | 通常由 Web 服务器提供 `80/443` |

生产环境应由浏览器访问 Web 服务器，由 Web 服务器把 `/api` 转发给后端；数据库只允许后端所在的受信网络访问。

## 9. 部署后验证

### 9.1 自动检查

```powershell
Push-Location .\backend
try { .\mvnw.cmd verify } finally { Pop-Location }

Push-Location .\front-end
try {
    node.exe --test tests/*.test.js
    npm.cmd run build
} finally { Pop-Location }
```

仅在一键演示的隔离数据库中，可在服务启动后运行：

```powershell
python.exe .\scripts\stage1-smoke.py http://127.0.0.1:5180/api/v1
```

该脚本会创建虚构测试记录，不得对生产数据库执行。

### 9.2 数据库与页面检查

V1～V7 的当前结构应包含 14 张基础表。可使用目标数据库账号交互式输入密码后检查：

```powershell
mysql.exe `
    --protocol=TCP `
    --host=127.0.0.1 `
    --port=3306 `
    "--user=$env:DB_USERNAME" `
    --password `
    --database=elm_lite `
    --execute="SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='elm_lite' AND table_type='BASE TABLE';"
```

随后至少检查：

- `/api/v1/shops` 返回统一响应且 `code=0`。
- 首页和直接访问的前端路由均能加载。
- 用户、商家、骑手和管理员入口符合[本地演示指南](demo.md)及[接口契约](../api-contract.md)。
- 后端日志没有数据库连接、迁移缺失或 JWT 配置错误。

## 10. 停止、重启与清理

- 一键演示：在启动终端按 Enter，由脚本按顺序停止三个服务。
- 手动前后端：在各自前台终端按 `Ctrl+C` 正常停止。
- 外部 MySQL：使用操作系统服务管理工具或数据库运维流程停止，不直接结束未知 `mysqld` 进程。
- 普通重启不会删除业务数据库；一键演示重启也会保留 `.local-demo/` 中的数据。
- 若确需重新验证全新演示库，应先停止脚本，再备份或重命名 `.local-demo/`，不得在服务运行时删除数据目录。

完成本地手动部署后，可按需清除当前 PowerShell 会话中的敏感环境变量：

```powershell
Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
Remove-Item Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

## 11. 常见问题

| 现象 | 检查与处理 |
| --- | --- |
| `java`、`node`、`python` 或 MySQL 命令无法识别 | 检查对应安装目录是否加入 PATH，重新打开终端后再运行 `Get-Command` |
| `npm.ps1` 被执行策略阻止 | 使用仓库文档中的 `npm.cmd` |
| Maven Wrapper 或 `npm ci` 下载失败 | 检查首次安装所需网络、代理和证书配置，不提交本机镜像凭据 |
| 一键脚本提示端口占用 | 使用 `Get-NetTCPConnection` 确认进程，正常停止旧演示后重试 |
| MySQL `Access denied` | 核对 `DB_USERNAME`、`DB_PASSWORD`、账号权限及目标端口 |
| 后端数据库连接失败 | 核对 `DB_URL`、MySQL 是否监听、数据库是否存在及时区参数 |
| 迁移提示表或字段已存在 | 确认是否对非全新库重复执行迁移；不要修改已应用迁移或关闭外键检查掩盖问题 |
| 前端请求 `/api` 失败 | 本地检查 `API_PROXY_TARGET` 和后端端口；生产检查 Web 服务器反向代理 |
| 直接刷新前端路由返回 404 | 为 history 路由配置回退到 `index.html` |
| 演示重启后返回 401 | 演示 JWT 密钥已重新生成，退出后重新登录 |
| 一键演示启动失败 | 查看 `.local-demo/` 中对应的 `*-error.log` 和 `verify.log` |

## 12. 生产安全要求

1. 不运行 `scripts/start-demo.ps1`，不使用它创建的空密码 MySQL。
2. 不导入虚构演示数据，不使用 root 作为应用数据库账号；按最小权限创建独立账号。
3. 通过受控环境变量或密钥管理系统注入数据库密码、JWT 密钥和管理员初始化密码。
4. JWT 密钥使用足够强度的随机值并建立轮换流程；轮换会使已有登录令牌失效。
5. 管理员初始化完成后移除 `ADMIN_USERNAME`、`ADMIN_PASSWORD`，并限制管理员入口访问。
6. 数据库迁移前备份并验证恢复方案；迁移文件一经应用不得修改。
7. 仅通过 HTTPS 暴露 Web 服务；限制后端管理面和数据库的网络访问。
8. 由实际部署平台负责进程守护、日志轮换、监控、备份、TLS 证书和 `/api` 反向代理配置。

## 13. 当前验证基线

本文档编写时已在上表所列 Windows 11 环境完成以下检查：

- Maven Wrapper、Java、Node.js、npm、Python 和 MySQL 版本检查通过。
- `scripts/start-demo.ps1` 成功执行后端验证、隔离数据库初始化、V1～V7 迁移、演示目录补齐和真实 HTTP 就绪检查。
- V1～V7 最终结构为 14 张基础表、18 个外键和 45 个索引。
- 一键演示重复启动时不重复导入店铺和商品，数据能够保留。

后续依赖、端口、脚本或迁移发生变化时，应同步更新本文档并重新执行部署验证。
