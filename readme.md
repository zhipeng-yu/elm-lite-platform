# elm-lite-platform

三人小组在 2026 小学期完成的轻量级外卖服务平台，参考饿了么核心业务流程。项目重点是通过 TDD、前后端分离、REST 接口和 Git 协作，交付一套可演示、可测试、可迭代的系统。

## 文档入口

- [成员协作指南](CONTRIBUTING.md)：分工、PowerShell Git 步骤、PR、审核和冲突处理
- [AI 协作规则](AGENTS.md)：AI 必须遵守的 TDD、架构和修改边界
- [接口契约与登录鉴权方案](api-contract.md)：统一响应、用户/商家、商品、地址、购物车、订单接口和 JWT 约定
- [每日任务看板](handoff.md)：三人每日分工、完成状态、PR、阻塞和下一步
- [本地演示指南](docs/demo.md)：独立 MySQL 启动、商家及用户演示步骤、真实 HTTP 检查
- `Instruction Manual/26271学期-软件工程综合实践.md`：2026 年课程总要求

四份 2021 年 PDF 仅用于参考页面、业务流程和表结构。除非老师明确要求，默认只开发一套最终的前后端分离项目。

## 打开网页做演示（Windows）

先安装 JDK 21、Node.js、Python 3 和 MySQL 8.4，并确保 `java`、`node`、`npm.cmd`、`python`、`mysql.exe`、`mysqld.exe` 可在终端运行。首次下载 Maven 和前端依赖需要网络。

1. 用 VS Code 打开项目文件夹，选择“终端 → 新建终端”，在**仓库根目录**（能看到 `scripts`、`backend`、`front-end`）执行：

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-demo.ps1
   ```

2. 等待终端显示 `Demo ready: http://127.0.0.1:5180`。脚本会先执行后端测试，再准备独立数据库、至少 30 家店铺及餐品，最后启动前后端；无需另外手动启动数据库或运行 `npm run dev`。
3. 在本机 Edge 或 Chrome 地址栏输入 **<http://127.0.0.1:5180/home>**。保持启动终端运行，不要在演示过程中按 Enter。
4. 演示结束，回到启动终端，按 **Enter** 停止服务。下次执行同一命令即可；数据保留，重启后需要重新登录。

| 演示入口 | 地址与用法 |
| --- | --- |
| 首页与店铺浏览 | <http://127.0.0.1:5180/home>；无需登录，可搜索店铺、筛选及排序 |
| 用户注册与登录 | <http://127.0.0.1:5180/register> / <http://127.0.0.1:5180/login>；自行注册演示账号，再展示地址、购物车和下单 |
| 商家入口 | <http://127.0.0.1:5180/merchant/login>；可切换注册，登录后进入工作台，创建店铺、分类和商品 |

电脑端直接展示完整窗口；展示手机布局时，在 Edge/Chrome 按 `F12`，再按 `Ctrl+Shift+M` 切换设备模拟，设置约 `390 × 844`。当前服务只监听本机，手机设备模拟在电脑浏览器中进行；实体手机不能通过该 `127.0.0.1` 地址访问电脑服务。

打不开页面时，先确认终端已显示 `Demo ready` 且仍在运行；服务已运行就直接打开网址，不要重复启动。若提示端口占用，先回到原启动终端按 Enter 停止；若启动失败，查看 `backend/target/local-demo/` 中的 `verify.log`、`backend.log`、`frontend.log` 及对应错误日志。详细操作顺序见 [本地演示指南](docs/demo.md)。

## 技术栈

- 后端：JDK 21、Spring Boot 3.5.16、Maven 3.9.16、MyBatis-Plus 3.5.17
- 前端：Vue 3、Vue Router、Axios、Element Plus
- 数据库：MySQL
- 测试：JUnit 5、Spring Boot Test、MockMvc、H2、JaCoCo 0.8.15
- 接口：REST 风格，统一 `/api/v1` 前缀和 `code/msg/data` 响应结构

项目运行时版本以仓库中的构建配置为准；个人使用的编辑器、数据库客户端等辅助工具不要求统一版本。未经小组确认不要更换框架或增加依赖。

JDBC 和 Servlet 作为 Spring Boot 底层技术使用，不单独作为阶段成果提交。

## 后端开发

后端代码位于 `backend/`，根包名为 `com.elmlite.platform`。Windows PowerShell 中执行：

```powershell
Set-Location .\backend
.\mvnw.cmd test
.\mvnw.cmd verify
```

`verify` 会运行测试并在 `backend/target/site/jacoco/index.html` 生成覆盖率报告。测试使用 H2 的 MySQL 兼容模式，不依赖本地 MySQL。

启动应用前需要依次执行 `database/migration/` 中的 V1、V2 迁移，再导入 `database/init/V1__seed_data.sql`，并在当前终端提供本机数据库凭据：

```powershell
$env:DB_USERNAME = '<本机 MySQL 用户名>'
$env:DB_PASSWORD = '<本机 MySQL 密码>'
$env:JWT_SECRET = '<本机生成的至少32字节随机密钥>'
.\mvnw.cmd spring-boot:run
```

将示例占位符替换为本机配置；密钥不得提交到 Git。`JAVA_HOME` 应指向 JDK 21 安装目录。

## 前端开发与联调

前端位于 `front-end/`，先安装锁定的依赖：

```powershell
Set-Location .\front-end
npm.cmd ci
$env:VITE_USE_MOCK = 'false'
npm.cmd run dev
```

真实联调前先启动后端；Vite 将 `/api` 转发到 `http://localhost:8080`，可用 `API_PROXY_TARGET` 指定其他后端地址。默认使用真实接口；原有用户端模拟页面验证时将 `VITE_USE_MOCK` 改为 `'true'` 并重启 Vite。商家管理与个人信息页面使用真实接口。切换模式后退出登录再重新登录，模拟 Token 无法用于真实后端；模拟账号、地址修改和登录会话只保存在内存，刷新后恢复初始数据并需要重新登录。新注册用户的模拟地址列表为空，演示地址只属于 `demo`。

前端检查：`node --test tests/*.test.js` 验证 mock/真实 HTTP 分流、地址契约、分类切换请求顺序、购物车/订单 mock 流程、商家表单与登录跳转、店铺搜索/筛选/排序，`npm.cmd run build` 检查生产构建。上述自动化测试不代替关闭 mock 后的真实后端联调。PowerShell 使用 `npm.cmd` 可避免本机脚本执行策略拦截 `npm.ps1`。`npm.cmd run preview` 仅预览构建产物，部署时需由 Web 服务器配置 `/api` 转发。

## 基础功能

- 用户注册、登录和个人信息
- 商家、店铺及营业状态
- 商品分类、价格和库存
- 购物车和收货地址
- 订单创建、状态查询和历史订单
- 支付信息展示，不接入真实支付

需求扩展范围：手机端页面优化、商品多图与介绍、商家订单处理、取消订单恢复库存、店铺优惠券、骑手端和管理员端。接口设计见 `api-contract.md`，各项实现状态和每日分工见 `handoff.md`。

组员首次参与请先完整阅读 [CONTRIBUTING.md](CONTRIBUTING.md)，再克隆仓库和创建功能分支。

## 三周目标

- 第一周：架构、数据库、基础功能和基础测试。
- 第二周：需求变更、增量测试和完整回归。
- 第三周：重构、页面完善、交叉验收和交付材料。

核心接口覆盖率要求 100%，关键业务方法覆盖率不低于 90%。必须保留测试类、测试日志、覆盖率报告和需求变更后的回归记录。
