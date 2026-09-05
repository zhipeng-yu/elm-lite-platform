# 成员每日任务与完成情况

最后更新：2026-09-06

本文件只维护每日任务进度、当前阻塞和 AI 使用记录；长期分工与协作规则见 `CONTRIBUTING.md`，已确认的接口方案见 `api-contract.md`。

## D1—D8 任务看板

原 D2—D15 压缩为 D2—D8；每天先合并前一天 PR，再开始当天任务。

| 开发日 | 余（A） | 梁（B） | 龙（C） |
| --- | --- | --- | --- |
| D1<br>日期：2026-09-02 | ✅ 已完成：Issue [#2](https://github.com/zhipeng-yu/elm-lite-platform/issues/2)，建立 Spring Boot 后端脚手架、分层目录、测试基础配置和依赖基线；PR [#6](https://github.com/zhipeng-yu/elm-lite-platform/pull/6) 已合并 | ✅ 已完成：设计 ER 图，建立 V1 数据库迁移、初始化 SQL 和字段说明；PR [#5](https://github.com/zhipeng-yu/elm-lite-platform/pull/5) 已合并 | ✅ 已完成：Issue [#10](https://github.com/zhipeng-yu/elm-lite-platform/issues/10)，建立 Vue 3 前端脚手架、Router 和页面目录；PR [#11](https://github.com/zhipeng-yu/elm-lite-platform/pull/11) 已合并 |
| D2<br>日期：2026-09-03 | ✅ 已完成：Issue [#12](https://github.com/zhipeng-yu/elm-lite-platform/issues/12)，按红—绿—重构流程实现统一响应和异常处理；`verify` 6/6 通过；PR [#13](https://github.com/zhipeng-yu/elm-lite-platform/pull/13) 已合并 | ✅ 已完成：建立 Entity、Mapper、V2 数据库迁移和可重复的 H2 测试数据；PR [#15](https://github.com/zhipeng-yu/elm-lite-platform/pull/15) 已合并 | ✅ 已完成：封装 Axios、认证头和统一错误处理，完成基础布局及加载、空数据、错误状态模拟页面；PR [#17](https://github.com/zhipeng-yu/elm-lite-platform/pull/17) 已合并 |
| D3<br>日期：2026-09-04 | ✅ 已完成：Issue [#22](https://github.com/zhipeng-yu/elm-lite-platform/issues/22)，用户注册、登录、JWT 鉴权和个人信息；用户模块回归 22/22 通过；PR [#24](https://github.com/zhipeng-yu/elm-lite-platform/pull/24) 已合并 | ✅ 已完成：商家注册、独立登录、店铺创建及营业状态和归属校验；Mapper 扫描隔离已修复；PR [#25](https://github.com/zhipeng-yu/elm-lite-platform/pull/25) 已合并 | ✅ 已完成：注册、登录、店铺列表和详情页面及模拟数据；PR [#21](https://github.com/zhipeng-yu/elm-lite-platform/pull/21) 已合并 |
| D4<br>日期：2026-09-05 | ✅ 已完成：Issue [#30](https://github.com/zhipeng-yu/elm-lite-platform/issues/30)，地址增删改查、默认地址与公共鉴权；PR [#31](https://github.com/zhipeng-yu/elm-lite-platform/pull/31) 已合并 | ✅ 已完成：Issue [#32](https://github.com/zhipeng-yu/elm-lite-platform/issues/32)，公开分类/商品查询、商家分类/商品管理、价格库存与归属校验；PR [#33](https://github.com/zhipeng-yu/elm-lite-platform/pull/33) 已合并 | ✅ 已完成：Issue [#34](https://github.com/zhipeng-yu/elm-lite-platform/issues/34)，分类、商品列表/详情、地址页面与模拟接口；PR [#35](https://github.com/zhipeng-yu/elm-lite-platform/pull/35) 已合并 |
| D5<br>日期：2026-09-06 | ⬜ 待完成：按红—绿流程完成订单创建、列表和状态查询，回归 A 板块 | ⬜ 待完成：按红—绿流程完成购物车增删改查和下单所需库存校验，回归 B 板块 | ⬜ 待完成：完成购物车、下单和订单列表页面，跑通注册至订单查询的第一轮全流程 |
| D6<br>日期：待填写 | ⬜ 待完成：分析需求变更对 A 板块的影响，先提交失败测试，再完成最小实现和 A 板块回归 | ⬜ 待完成：分析需求变更对 B 板块及数据库的影响，先提交迁移与失败测试，再完成最小实现和 B 板块回归 | ⬜ 待完成：更新受影响页面、接口适配和验收场景，只修改 C 板块 |
| D7<br>日期：待填写 | ⬜ 待完成：执行 A 板块集成、权限、边界和覆盖率检查，只修复 A 问题并冻结后端公共契约 | ⬜ 待完成：执行 B 板块集成和边界回归，验证迁移可重复执行及全新数据库初始化，只修复 B 问题 | ⬜ 待完成：完成变更后的端到端联调和前端冒烟测试，补齐加载、空数据、校验和错误提示，按负责人登记问题 |
| D8<br>日期：待填写 | ⬜ 待完成：运行完整后端测试和覆盖率检查，检查 REST/统一响应规范，汇总 README、API 和技术交付说明 | ⬜ 待完成：执行干净环境数据库部署验证，准备演示数据、最终 SQL、数据字典和数据库说明 | ⬜ 待完成：执行最终演示与交叉验收，整理 Postman/ApiFox 集合、测试报告、需求变更记录、验收报告、截图和答辩材料 |

未完成任务顺延。D6 按老师公布的需求变更替换具体内容；D7 后原则上冻结接口和数据库结构。

## 2026-09-06：D4 合并后集成检查

- PR #31、#33、#35 已于 2026-09-06 07:25（北京时间）合并；本地 `main` 从 `27f71ea` 快进到 `6a8bede`，无 Git 文本冲突。原有未跟踪 `.vscode/` 保留且未提交。
- 本次集成任务：Issue [#36](https://github.com/zhipeng-yu/elm-lite-platform/issues/36)，分支 `fix/zp-day4-integration`。修复整数请求被截断、分类负排序与并发重名返回 500；修复默认地址取消、字段错误提示、模拟地址越权/非法写入，以及快速切分类时旧响应覆盖当前结果。未扩展 D5 功能、增加项目依赖或修改数据库结构。
- TDD 记录：前端地址测试提交 `1ccb0e8`（修复前 2/5 通过，3 项失败）；后端测试提交 `e84934d`（定向 33 项中新增 16 项失败）；商品切换测试提交 `d0cd69c`（4 项均先失败）。上述测试均早于实现提交。

| 实际检查 | 结果 |
| --- | --- |
| JDK 21、Maven 3.9.16 `-B verify` | 211/211 通过，0 失败/错误/跳过；JaCoCo 总行覆盖率 94.97% |
| `node --test tests/request.test.js tests/product-list.test.js` | 9/9 通过，覆盖 HTTP 分流、地址契约和请求竞态 |
| `npm.cmd run build` | 通过；仍有已有的单个产物超过 500 kB 提示，不影响构建 |
| Headless Chrome 页面检查 | 登录、取消默认、去空白校验、字段错误展示、新增/取消删除/删除默认、店铺→分类→商品列表→详情均通过，无页面 JavaScript 错误 |
| Git 检查 | `git diff --check` 通过，无冲突标记；课程原文件、构建依赖和 `.vscode/` 未修改 |

后端使用 H2；浏览器使用开发 mock，字段错误场景注入 HTTP 400 形状的响应，不能据此宣称真实 MySQL 联调或人工交叉验收已通过。Maven Wrapper 本机出现一次 `JAVA_HOME` 识别失败，最终使用同版已缓存 Maven 和验证过的 JDK 21 完成 `verify`。日志、浏览器脚本与截图保留在本机 `backend/target/day4-*`，覆盖率报告位于 `backend/target/site/jacoco/index.html`，未将临时工具和日志提交到 Git。

## 当前阻塞

- 暂无代码集成阻塞；下一步三人同步最新 `main` 后按 D5 分工开始任务。
- 真实 MySQL 并发/外键复验、关闭 mock 的浏览器联调和人工交叉验收尚未执行。分类/商品 Service 行覆盖率分别为 80.00%/86.73%，后续按课程口径补齐覆盖；本次未将全部测试通过等同于最终验收完成。

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
| 2026-09-04 | 辅助按红—绿流程实现 D3 用户注册、用户登录、JWT 身份区分和个人信息接口，并同步接口契约 | `backend/pom.xml`、`backend/src/main/java/com/elmlite/platform/config/SecurityConfig.java`、`backend/src/main/java/com/elmlite/platform/controller/`、`backend/src/main/java/com/elmlite/platform/service/`、`backend/src/main/resources/application.yml`、`backend/src/test/`、`api-contract.md`、`handoff.md` | 待余复核 |
| 2026-09-05 | 按用户授权自主检查并修复 D4 开发前的 D3 联调障碍，保留红绿测试与 PR 发布记录 | `ShopController.java`、`ShopService.java`、`GlobalExceptionHandler.java`、`PublicShopTest.java`、`front-end/src/api/request.js`、`RegisterView.vue`、`front-end/tests/request.test.js`、`readme.md`、`docs/database/data-dictionary.md`、`handoff.md` | 待余/梁/龙返岗复核；本次无人工复核，不冒记已审核 |
| 2026-09-05 | 将三人已同意的 Day4 清单同步到接口契约、数据库业务说明及任务状态，记录分类唯一与地址删除例外 | `api-contract.md`、`docs/database/data-dictionary.md`、`docs/database/er-diagram.md`、`handoff.md` | 余转述三人已确认清单；本次文档同步待余复核 |
| 2026-09-05 | 按三阶段红—绿流程实现余的 D4 地址与公共鉴权，精简进度记录 | `AddressController.java`、`AddressService.java`、`AddressRequest.java`、`UserMapper.java`、`SecurityConfig.java`、`GlobalExceptionHandler.java`、`AddressApiTest.java`、`Day4SecurityTest.java`、`address-api-schema.sql`、`handoff.md` | 待余复核 |
| 2026-09-06 | 按用户全程授权同步 D4 合并结果，先复现再修复接口与页面集成问题，执行后端/前端/浏览器检查并更新交接 | 分类 Controller/Service、主/测试 Jackson 配置、分类/商品测试、`request.js`、地址/商品列表页面、前端测试、`readme.md`、`api-contract.md`、数据库字段说明、`handoff.md` | 待余/梁/龙返岗复核；已获自主执行及合并授权，本次未冒记人工审核或交叉验收 |
