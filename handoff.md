# 成员每日任务与完成情况

最后更新：2026-09-08

本文件只维护每日任务进度、当前阻塞和 AI 使用记录；长期分工与协作规则见 `CONTRIBUTING.md`，已确认的接口方案见 `api-contract.md`。

## D1—D8 任务看板

原 D2—D15 压缩为 D2—D8；每天先合并前一天 PR，再开始当天任务。

| 开发日 | 余（A） | 梁（B） | 龙（C） |
| --- | --- | --- | --- |
| D1<br>日期：2026-09-02 | ✅ 已完成：Issue [#2](https://github.com/zhipeng-yu/elm-lite-platform/issues/2)，建立 Spring Boot 后端脚手架、分层目录、测试基础配置和依赖基线；PR [#6](https://github.com/zhipeng-yu/elm-lite-platform/pull/6) 已合并 | ✅ 已完成：设计 ER 图，建立 V1 数据库迁移、初始化 SQL 和字段说明；PR [#5](https://github.com/zhipeng-yu/elm-lite-platform/pull/5) 已合并 | ✅ 已完成：Issue [#10](https://github.com/zhipeng-yu/elm-lite-platform/issues/10)，建立 Vue 3 前端脚手架、Router 和页面目录；PR [#11](https://github.com/zhipeng-yu/elm-lite-platform/pull/11) 已合并 |
| D2<br>日期：2026-09-03 | ✅ 已完成：Issue [#12](https://github.com/zhipeng-yu/elm-lite-platform/issues/12)，按红—绿—重构流程实现统一响应和异常处理；`verify` 6/6 通过；PR [#13](https://github.com/zhipeng-yu/elm-lite-platform/pull/13) 已合并 | ✅ 已完成：建立 Entity、Mapper、V2 数据库迁移和可重复的 H2 测试数据；PR [#15](https://github.com/zhipeng-yu/elm-lite-platform/pull/15) 已合并 | ✅ 已完成：封装 Axios、认证头和统一错误处理，完成基础布局及加载、空数据、错误状态模拟页面；PR [#17](https://github.com/zhipeng-yu/elm-lite-platform/pull/17) 已合并 |
| D3<br>日期：2026-09-04 | ✅ 已完成：Issue [#22](https://github.com/zhipeng-yu/elm-lite-platform/issues/22)，用户注册、登录、JWT 鉴权和个人信息；用户模块回归 22/22 通过；PR [#24](https://github.com/zhipeng-yu/elm-lite-platform/pull/24) 已合并 | ✅ 已完成：商家注册、独立登录、店铺创建及营业状态和归属校验；Mapper 扫描隔离已修复；PR [#25](https://github.com/zhipeng-yu/elm-lite-platform/pull/25) 已合并 | ✅ 已完成：注册、登录、店铺列表和详情页面及模拟数据；PR [#21](https://github.com/zhipeng-yu/elm-lite-platform/pull/21) 已合并 |
| D4<br>日期：2026-09-05 | ✅ 已完成：Issue [#30](https://github.com/zhipeng-yu/elm-lite-platform/issues/30)，地址增删改查、默认地址与公共鉴权；PR [#31](https://github.com/zhipeng-yu/elm-lite-platform/pull/31) 已合并 | ✅ 已完成：Issue [#32](https://github.com/zhipeng-yu/elm-lite-platform/issues/32)，公开分类/商品查询、商家分类/商品管理、价格库存与归属校验；PR [#33](https://github.com/zhipeng-yu/elm-lite-platform/pull/33) 已合并 | ✅ 已完成：Issue [#34](https://github.com/zhipeng-yu/elm-lite-platform/issues/34)，分类、商品列表/详情、地址页面与模拟接口；PR [#35](https://github.com/zhipeng-yu/elm-lite-platform/pull/35) 已合并 |
| D5<br>日期：2026-09-06 | ✅ 开发完成并合并：Issue [#39](https://github.com/zhipeng-yu/elm-lite-platform/issues/39)，PR [#40](https://github.com/zhipeng-yu/elm-lite-platform/pull/40)；订单创建、列表/详情、公共鉴权及库存/清理事务对接已完成；集成回归 `verify` 304/304 通过，保留红—绿提交 | ✅ 开发完成并合并：Issue [#38](https://github.com/zhipeng-yu/elm-lite-platform/issues/38)，PR [#41](https://github.com/zhipeng-yu/elm-lite-platform/pull/41)；购物车 CRUD、单店/归属/库存校验及并发丢数量、锁顺序、数量溢出修复完成；CartService 行覆盖率 97.39%，分类/商品管理 Service 均超过 90% | ✅ 页面开发已合并，真实联调于 D6 补齐：PR [#43](https://github.com/zhipeng-yu/elm-lite-platform/pull/43)；购物车、下单、订单列表/详情及 mock 全流程已实现；前端测试 12/12、生产构建通过；关闭 mock 的真实 HTTP 与浏览器主流程已于 D6 验证，人工复核另行完成 |
| D6<br>日期：2026-09-07 | 🟨 阶段一收尾：Issue [#47](https://github.com/zhipeng-yu/elm-lite-platform/issues/47)；余获三人授权统一推进；后端 316/316、核心公开业务方法行覆盖率达标，补齐演示指南与启动脚本；PR [#48](https://github.com/zhipeng-yu/elm-lite-platform/pull/48) 已提交，浏览器主流程通过，待人工复核 | 🟨 商家管理查询已按红—绿提交；真实 MySQL 两次全新初始化及 seed 重复导入、并发库存、默认地址、外键快照检查通过；旧辅助方法清理 PR [#46](https://github.com/zhipeng-yu/elm-lite-platform/pull/46) 已合并 main，与本分支的集成验证待完成 | 🟨 个人信息及商家注册登录、店铺/分类/商品管理页面已补齐；前端 17/17、生产构建通过；按确认改为蓝白点餐首页/店铺列表/导航，补至 30 家店、每家至少 6 件餐品；电脑和手机点击搜索筛选、新店下单 ¥12 通过；人工演示验收待完成 |
| D7<br>日期：2026-09-08 | 🟨 已核对远端 PR、推送与拉取恢复正常，并同步交接和验证文档、补充 README 网页演示启动步骤；待执行最新 main 集成、权限、边界和覆盖率检查，再冻结后端公共契约 | ⬜ 待完成：执行 B 板块集成和边界回归，验证全新数据库按序应用迁移及种子数据重复导入，只修复 B 问题 | ⬜ 待完成：完成变更后的端到端联调和前端冒烟测试，补齐加载、空数据、校验和错误提示，按负责人登记问题 |
| D8<br>日期：待填写 | ⬜ 待完成：运行完整后端测试和覆盖率检查，检查 REST/统一响应规范，汇总 README、API 和技术交付说明 | ⬜ 待完成：执行干净环境数据库部署验证，准备演示数据、最终 SQL、数据字典和数据库说明 | ⬜ 待完成：执行最终演示与交叉验收，整理 Postman/ApiFox 集合、测试报告、需求变更记录、验收报告、截图和答辩材料 |

未完成任务顺延。按余补充的老师通知，9 月 9 日中期检查前优先完成阶段一展示；需求变更在中期检查后按老师实际公布内容及小组确认安排，不预设变更。D7/D8 验证与材料任务按剩余工作执行。

## 当前阻塞

- 真实 MySQL、关闭 mock 的 HTTP 流程和两种浏览器实际点击主流程均已验证；连接阻塞已解除。组内人工复核及课程要求的组间交叉验收仍待完成，详细证据见 [阶段一验证报告](docs/testing/stage1-report.md)。
- 最终代码、测试断言及结果待余/梁/龙复核；PR #46 已合并 main，PR [#48](https://github.com/zhipeng-yu/elm-lite-platform/pull/48) 已合并。已有 316/316 后端结果属于合并前的阶段一功能分支，合并后的集成验证待完成。README 演示说明提交晚于 PR 合并，现已补入当前项目的 `docs/yu-demo-readme` 分支，尚未合入 main。

## AI 使用记录

| 日期 | 用途 | 涉及文件 | 人工复核人 |
| --- | --- | --- | --- |
| 2026-09-08 | 按余要求将遗漏的 README 演示说明保留到当前项目，删除旧仓库目录，修正 PR 合并状态；后续使用当前工作区 | `readme.md`、`handoff.md`；旧目录 `D:/Project/ledu_project/elm-lite-platform` | 余已授权删除；文档待余复核，未重跑业务测试 |
| 2026-09-08 | 核实 PR 状态及 Git 推送/拉取，清理过期进度，统一测试数量、验证范围与人工验收口径，补充 README 启动命令、网页入口、手机布局展示和停止方法，更新原 PR | `handoff.md`、`readme.md`、`docs/testing/stage1-report.md`、PR #48 描述 | 待余复核；本次为文档更新，未重跑业务测试 |
| 2026-09-07 | 按余转述三人授权补齐阶段一管理查询与页面，保留红—绿提交，执行真实 MySQL/HTTP、回归与覆盖率检查，准备本地演示；按 grilling 确认范围后改蓝白首页/列表/导航、补 30 家店，执行手机和电脑点击；遵循 ponytail，不新增依赖 | 商家 Controller/Service 与测试、前端页面/API/认证/样式/测试、`scripts/`、演示餐品 JSON/本地图片、`docs/demo.md`、`docs/testing/stage1-report.md`、`api-contract.md`、`readme.md`、`handoff.md` | 待余/梁/龙复核；浏览器点击主流程已通过，人工验收未完成，未冒记人工结论 |
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
| 2026-09-04 | 辅助按红—绿流程实现 D3 用户注册、用户登录、JWT 身份区分和个人信息接口，并同步接口契约 | `backend/pom.xml`、`backend/src/main/java/com/elmlite/platform/config/SecurityConfig.java`、`backend/src/main/java/com/elmlite/platform/controller/`、`backend/src/main/java/com/elmlite/platform/service/`、`backend/src/main/resources/application.yml`、`backend/src/test/`、`api-contract.md`、`handoff.md` | 待余复核 |
| 2026-09-05 | 按用户授权自主检查并修复 D4 开发前的 D3 联调障碍，保留红绿测试与 PR 发布记录 | `ShopController.java`、`ShopService.java`、`GlobalExceptionHandler.java`、`PublicShopTest.java`、`front-end/src/api/request.js`、`RegisterView.vue`、`front-end/tests/request.test.js`、`readme.md`、`docs/database/data-dictionary.md`、`handoff.md` | 待余/梁/龙返岗复核；本次无人工复核，不冒记已审核 |
| 2026-09-05 | 将三人已同意的 Day4 清单同步到接口契约、数据库业务说明及任务状态，记录分类唯一与地址删除例外 | `api-contract.md`、`docs/database/data-dictionary.md`、`docs/database/er-diagram.md`、`handoff.md` | 余转述三人已确认清单；本次文档同步待余复核 |
| 2026-09-05 | 按三阶段红—绿流程实现余的 D4 地址与公共鉴权，精简进度记录 | `AddressController.java`、`AddressService.java`、`AddressRequest.java`、`UserMapper.java`、`SecurityConfig.java`、`GlobalExceptionHandler.java`、`AddressApiTest.java`、`Day4SecurityTest.java`、`address-api-schema.sql`、`handoff.md` | 待余复核 |
| 2026-09-06 | 按用户全程授权同步 D4 合并结果，先复现再修复接口与页面集成问题，执行后端/前端/浏览器检查并更新交接 | 分类 Controller/Service、主/测试 Jackson 配置、分类/商品测试、`request.js`、地址/商品列表页面、前端测试、`readme.md`、`api-contract.md`、数据库字段说明、`handoff.md` | 待余/梁/龙返岗复核；已获自主执行及合并授权，本次未冒记人工审核或交叉验收 |
| 2026-09-06 | 辅助 D5 订单与鉴权的红—绿开发、库存/清理对接和库存覆盖修复，执行回归并同步文档；按余要求删除交接流水章节、固化禁止新增规则 | 订单 Controller/DTO/Service、`CheckoutService`、`ProductMapper`、`MerchantProductService`、`SecurityConfig`、D5 测试及 H2 数据、`AGENTS.md`、`api-contract.md`、数据库字段说明、`handoff.md` | 余已确认需求及前三批测试断言，并授权后续自主处理和提交 PR；补充测试、最终代码及结果待余/梁复核，未冒记人工验收 |
| 2026-09-07 | 按用户要求解决 PR #41 与最新 main 的冲突，保留 main 的商品行锁、原子扣库存及更新时间；执行后端和前端回归 | `ProductMapper.java`、`handoff.md`；其余为 main 合入内容 | 待余/梁复核；本次未代替人工审核 |
| 2026-09-07 | 审核 PR #41；先以 5 个失败测试复现用户锁缺失、并发丢数量和溢出 500，再最小修复并执行完整回归；红绿证据见 PR 描述及 `backend/target/pr41-review-*.log` | `CartService.java`、`CartApiTest.java`、`CartWriteLockTest.java`、`handoff.md` | 余已确认新增测试断言并授权修复后合并；AI 辅助审核不冒充人工交叉验收 |
| 2026-09-07 | 核实 D5 三个 PR 合并状态，清理过期阻塞并同步测试命令、数量溢出契约；检查文档差异与引用路径 | `handoff.md`、`readme.md`、`api-contract.md` | 余授权本次纯文档更新，后改为通过 PR 提交；文字待余复核 |
