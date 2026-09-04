# 成员每日任务与完成情况

最后更新：2026-09-04

本文件只维护每日任务进度、当前阻塞和 AI 使用记录；长期分工与协作规则见 `CONTRIBUTING.md`，已确认的接口方案见 `api-contract.md`。

## D1—D8 任务看板

原 D2—D15 压缩为 D2—D8；每天先合并前一天 PR，再开始当天任务。

| 开发日 | 余（A） | 梁（B） | 龙（C） |
| --- | --- | --- | --- |
| D1<br>日期：2026-09-02 | ✅ 已完成：Issue [#2](https://github.com/zhipeng-yu/elm-lite-platform/issues/2)，建立 Spring Boot 后端脚手架、分层目录、测试基础配置和依赖基线；PR [#6](https://github.com/zhipeng-yu/elm-lite-platform/pull/6) 已合并 | ✅ 已完成：设计 ER 图，建立 V1 数据库迁移、初始化 SQL 和字段说明；PR [#5](https://github.com/zhipeng-yu/elm-lite-platform/pull/5) 已合并 | ✅ 已完成：Issue [#10](https://github.com/zhipeng-yu/elm-lite-platform/issues/10)，建立 Vue 3 前端脚手架、Router 和页面目录；PR [#11](https://github.com/zhipeng-yu/elm-lite-platform/pull/11) 已合并 |
| D2<br>日期：2026-09-03 | ✅ 已完成：Issue [#12](https://github.com/zhipeng-yu/elm-lite-platform/issues/12)，按红—绿—重构流程实现统一响应和异常处理；`verify` 6/6 通过；PR [#13](https://github.com/zhipeng-yu/elm-lite-platform/pull/13) 已合并 | ✅ 已完成：建立 Entity、Mapper、V2 数据库迁移和可重复的 H2 测试数据；PR [#15](https://github.com/zhipeng-yu/elm-lite-platform/pull/15) 已合并 | ✅ 已完成：封装 Axios、认证头和统一错误处理，完成基础布局及加载、空数据、错误状态模拟页面；PR [#17](https://github.com/zhipeng-yu/elm-lite-platform/pull/17) 已合并 |
| D3<br>日期：待填写 | ⬜ 待完成：按红—绿流程完成用户注册、登录、鉴权和个人信息 | ⬜ 待完成：按红—绿流程完成商家注册及店铺开店、关店、临时闭店 | ⬜ 待完成：完成注册、登录、店铺列表和店铺详情页面，按冻结契约使用模拟数据 |
| D4<br>日期：待填写 | ⬜ 待完成：按红—绿流程完成收货地址增删改查，覆盖权限、参数和边界场景 | ⬜ 待完成：按红—绿流程完成分类、商品列表、详情、价格和库存 | ⬜ 待完成：完成分类、商品列表、商品详情和地址页面，并联调已合并接口 |
| D5<br>日期：待填写 | ⬜ 待完成：按红—绿流程完成订单创建、列表和状态查询，回归 A 板块 | ⬜ 待完成：按红—绿流程完成购物车增删改查和下单所需库存校验，回归 B 板块 | ⬜ 待完成：完成购物车、下单和订单列表页面，跑通注册至订单查询的第一轮全流程 |
| D6<br>日期：待填写 | ⬜ 待完成：分析需求变更对 A 板块的影响，先提交失败测试，再完成最小实现和 A 板块回归 | ⬜ 待完成：分析需求变更对 B 板块及数据库的影响，先提交迁移与失败测试，再完成最小实现和 B 板块回归 | ⬜ 待完成：更新受影响页面、接口适配和验收场景，只修改 C 板块 |
| D7<br>日期：待填写 | ⬜ 待完成：执行 A 板块集成、权限、边界和覆盖率检查，只修复 A 问题并冻结后端公共契约 | ⬜ 待完成：执行 B 板块集成和边界回归，验证迁移可重复执行及全新数据库初始化，只修复 B 问题 | ⬜ 待完成：完成变更后的端到端联调和前端冒烟测试，补齐加载、空数据、校验和错误提示，按负责人登记问题 |
| D8<br>日期：待填写 | ⬜ 待完成：运行完整后端测试和覆盖率检查，检查 REST/统一响应规范，汇总 README、API 和技术交付说明 | ⬜ 待完成：执行干净环境数据库部署验证，准备演示数据、最终 SQL、数据字典和数据库说明 | ⬜ 待完成：执行最终演示与交叉验收，整理 Postman/ApiFox 集合、测试报告、需求变更记录、验收报告、截图和答辩材料 |

未完成任务顺延。D6 按老师公布的需求变更替换具体内容；D7 后原则上冻结接口和数据库结构。

## 当前阻塞

- 商家注册、开店和营业状态修改的鉴权方式尚待老师或小组确认；公开店铺列表和详情接口不受影响。

## AI 使用记录

| 日期 | 用途 | 涉及文件 | 人工复核人 |
| --- | --- | --- | --- |
| 2026-09-01 | 汇总课程要求并建立团队、Git 与 AI 协作规则 | `readme.md`、`AGENTS.md`、`handoff.md` | 待填写 |
| 2026-09-01 | 为 GitHub 新手补充可逐步执行的 PowerShell、VS Code、PR 和冲突处理流程 | `readme.md`、`handoff.md` | 待填写 |
| 2026-09-01 | 将稳定成员协作规则迁移到 CONTRIBUTING，并同步 README、AI 规则和交接记录 | `readme.md`、`CONTRIBUTING.md`、`AGENTS.md`、`handoff.md` | 待填写 |
| 2026-09-01 | 修复 README 被旧编辑器内容覆盖，恢复精简项目入口 | `readme.md`、`handoff.md` | 待填写 |
| 2026-09-01 | 将 handoff 重构为三人每日任务与完成状态看板，并落实 09:00 合并和错峰修改规则 | `handoff.md`、`CONTRIBUTING.md`、`readme.md` | 待填写 |
| 2026-09-02 | 辅助建立 D1 Spring Boot 脚手架、红—绿测试基线、Maven Wrapper 和启动说明 | `backend/`、`.gitignore`、`readme.md`、`handoff.md` | 待余复核 |
| 2026-09-02 | 将原 D2—D15 任务压缩为 D2—D8，并按模块和文件所有权划分三人并行边界 | `handoff.md` | 待余复核 |
| 2026-09-02 | 纠正成员 B、C 的姓名映射，统一为梁负责 B、龙负责 C | `handoff.md` | 待余复核 |
| 2026-09-02 | 记录三人确认的接口与 JWT 方案、精简看板并同步成员克隆和 D1 Issue 状态 | `api-contract.md`、`handoff.md` | 余 |
| 2026-09-02 | 补充每个任务对应一个 Issue、分支和 PR 的协作流程 | `CONTRIBUTING.md`、`handoff.md` | 余 |
| 2026-09-02 | 区分长期说明与短期进度，删除重复状态并修正成员职责和测试说明 | `readme.md`、`CONTRIBUTING.md`、`AGENTS.md`、`api-contract.md`、`handoff.md` | 待余复核 |
| 2026-09-03 | 检查 D1 合并后的 Git、后端构建、测试配置、数据库结构与接口契约，并更新 D1/D2 状态 | `handoff.md` | 余 |
| 2026-09-03 | 辅助按红—绿—重构流程实现 D2 统一响应、异常处理及 MockMvc 测试 | `backend/src/main/java/com/elmlite/platform/common/ApiResponse.java`、`backend/src/main/java/com/elmlite/platform/exception/`、`backend/src/test/java/com/elmlite/platform/common/GlobalExceptionHandlerTest.java`、`handoff.md` | 待余复核 |
| 2026-09-04 | 根据已合并 PR 更新 D2 三人任务完成状态 | `handoff.md` | 待余复核 |
| 2026-09-04 | 修复演示数据与 V2 用户字段不一致，并补充 D3 公开店铺接口契约 | `database/init/V1__seed_data.sql`、`readme.md`、`api-contract.md`、`handoff.md` | 待余复核 |
