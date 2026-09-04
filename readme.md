# elm-lite-platform

三人小组在 2026 小学期完成的轻量级外卖服务平台，参考饿了么核心业务流程。项目重点是通过 TDD、前后端分离、REST 接口和 Git 协作，交付一套可演示、可测试、可迭代的系统。

## 文档入口

- [成员协作指南](CONTRIBUTING.md)：分工、PowerShell Git 步骤、PR、审核和冲突处理
- [AI 协作规则](AGENTS.md)：AI 必须遵守的 TDD、架构和修改边界
- [接口契约与登录鉴权方案](api-contract.md)：统一响应、最小用户接口和 JWT 约定
- [每日任务看板](handoff.md)：三人每日分工、完成状态、PR、阻塞和下一步
- `Instruction Manual/26271学期-软件工程综合实践.md`：2026 年课程总要求

四份 2021 年 PDF 仅用于参考页面、业务流程和表结构。除非老师明确要求，默认只开发一套最终的前后端分离项目。

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
.\mvnw.cmd spring-boot:run
```

## 基础功能

- 用户注册、登录和个人信息
- 商家、店铺及营业状态
- 商品分类、价格和库存
- 购物车和收货地址
- 订单创建、状态查询和历史订单
- 支付信息展示，不接入真实支付

管理员后台属于可选功能，未经小组确认不要加入第一周范围。

组员首次参与请先完整阅读 [CONTRIBUTING.md](CONTRIBUTING.md)，再克隆仓库和创建功能分支。

## 三周目标

- 第一周：架构、数据库、基础功能和基础测试。
- 第二周：需求变更、增量测试和完整回归。
- 第三周：重构、页面完善、交叉验收和交付材料。

核心接口覆盖率要求 100%，关键业务方法覆盖率不低于 90%。必须保留测试类、测试日志、覆盖率报告和需求变更后的回归记录。
