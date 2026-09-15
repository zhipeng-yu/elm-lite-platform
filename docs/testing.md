# 项目测试文档

## 1. 测试目标

本文档说明项目的测试范围、环境、方法、关键用例和最终验证结果。测试覆盖后端业务与接口、前端交互与请求恢复、真实 MySQL 事务、四种身份权限、完整订单流程及生产构建。

需求验收项见[需求文档](requirements.md)，接口预期见[接口契约](../api-contract.md)，运行方法见[部署与运行指南](deployment.md)。

## 2. 最终验证基线

| 项目 | 验证值 |
| --- | --- |
| 日期 | 2026-09-15 |
| 操作系统 | Windows 11 amd64 |
| JDK | Eclipse Temurin 21.0.12.1 LTS |
| Maven Wrapper | Maven 3.9.16 |
| Spring Boot | 3.5.16 |
| Node.js | 24.20.0 |
| npm | 11.19.0 |
| Python | 3.13.15 |
| MySQL | 8.4.9 Community Server |
| 前端构建工具 | Vite 8.2.2 |
| 验证分支基线 | `main`，提交 `02cea38` |

测试账号、店铺和订单均使用虚构数据。密码和 JWT 密钥不记录在文档、仓库或日志中。

## 3. 测试层次与策略

| 层次 | 工具或入口 | 主要目标 |
| --- | --- | --- |
| 后端自动测试 | JUnit 5、Spring Boot Test、MockMvc、H2 | Service 规则、REST 契约、鉴权、事务和异常 |
| 后端覆盖率 | JaCoCo | 生成报告并执行 Maven 配置中的覆盖率门禁 |
| 前端自动测试 | Node.js Test Runner | mock/真实请求分流、表单、路由、交互和失败恢复 |
| 前端构建 | Vite | 验证全部页面和依赖可生成生产产物 |
| Stage 1 HTTP | `scripts/stage1-smoke.py` | 基础四端联调、回滚、库存并发和地址约束 |
| Stage 2 HTTP/数据库 | `scripts/stage2-smoke.py` | V1～V7、优惠券、履约、骑手、权限矩阵和演示数据幂等 |
| 人工页面验证 | 浏览器及 `docs/demo-screenshots/` | 页面展示、移动/桌面布局和完整业务操作 |

自动测试与真实环境验证互为补充：H2 测试提供快速回归，Stage 1/2 使用实际 Spring Boot、前端代理和 MySQL 8.4 验证集成行为。

## 4. 自动测试命令

### 4.1 后端

```powershell
Push-Location .\backend
try {
    .\mvnw.cmd verify
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed' }
} finally {
    Pop-Location
}
```

测试结果和构建结果由 Maven 汇总；JaCoCo 报告位于 `backend/target/site/jacoco/index.html`。

### 4.2 前端

```powershell
Push-Location .\front-end
try {
    node.exe --test tests/*.test.js
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed' }
    npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
} finally {
    Pop-Location
}
```

测试使用锁定依赖；生产构建产物生成在被 Git 忽略的 `front-end/dist/`。

## 5. 真实环境验证方法

### 5.1 启动隔离演示环境

在仓库根目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-demo.ps1
```

看到 `Demo ready: http://127.0.0.1:5180` 后保持启动终端运行。该环境使用回环地址上的 MySQL `13317`、后端 `18081` 和前端 `5180`，数据位于被 Git 忽略的 `.local-demo/`。

### 5.2 Stage 1

在第二个终端执行：

```powershell
python.exe .\scripts\stage1-smoke.py http://127.0.0.1:5180/api/v1
```

### 5.3 Stage 2

确保第二个终端解析到 MySQL 8.4 客户端：

```powershell
$mysql84 = Join-Path $env:ProgramFiles 'MySQL\MySQL Server 8.4\bin'
$env:Path = "$mysql84;$env:Path"
mysql.exe --version
python.exe .\scripts\stage2-smoke.py http://127.0.0.1:5180/api/v1
```

Stage 2 直接查询隔离 MySQL，并在执行前验证数据目录，避免误连其他数据库。完成后清理本次虚构账号、店铺和订单。

### 5.4 停止

回到启动终端按 Enter，脚本按顺序停止前端、后端和 MySQL。以下命令无输出表示端口已经释放：

```powershell
Get-NetTCPConnection -LocalPort 13317,18081,5180 -State Listen -ErrorAction SilentlyContinue
```

## 6. 自动测试结果

| 检查 | 最终结果 | 结论 |
| --- | --- | --- |
| `backend/mvnw.cmd verify` | 433 项测试，0 失败、0 错误、0 跳过 | 通过 |
| JaCoCo | 分析 124 个类，全部覆盖率门禁满足 | 通过 |
| `node.exe --test tests/*.test.js` | 57 项测试，57 通过、0 失败 | 通过 |
| `npm.cmd run build` | Vite 转换 1715 个模块并成功生成 `dist/` | 通过 |
| 启动脚本 | 最新 `main` 输出 `Demo ready` | 通过 |
| Stage 1 | 6 类真实 HTTP 检查完成 | 通过 |
| Stage 2 | 数据库和第二阶段业务检查完成，测试数据清理 | 通过；管理员真实检查按条件跳过 |

测试结束后 `git status --short` 无输出，构建和演示产物没有进入版本控制。

## 7. Stage 1 验证范围

Stage 1 在真实 HTTP 环境完成以下六类检查：

1. 商家注册登录、空店铺、新建店铺、分类和商品。
2. 用户注册登录、个人信息以及身份权限隔离。
3. 闭店下单失败回滚；成功下单的金额、库存和购物车清理；越权拒绝；删除地址后订单快照保留。
4. 下架商品与停用分类仍可在管理端找回并恢复。
5. 跨用户购物车占用和并发争抢最后一件库存：一单成功、一单返回冲突，不发生超卖。
6. 并发新增默认地址后，每个用户仍只有一个默认地址。

最终输出 `passed: 6`，并产生可追踪的虚构店铺和订单编号。

## 8. Stage 2 验证范围

Stage 2 在 Stage 1 基础上完成：

1. 核对 V1～V7 迁移形成的表、外键、引用、金额关系和分类归属。
2. 验证商品三张详情图、领券、用券、取消返券、重新使用优惠券以及重复取消旧订单。
3. 验证商家履约、骑手抢单、任务归属、配送详情和重复送达保护。
4. 检查 46 个受保护入口，执行 191 次匿名、错误 Token 和跨身份拒绝验证。
5. 重复补充演示数据，不污染手工店铺且不重置库存。
6. 清理本次产生的虚构账号、店铺和订单。

本次最终运行未提供 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD`，因此 Stage 2 明确报告“管理员检查未运行”。管理员登录、查询、停用和安全边界已经由后端自动测试覆盖；需要真实管理员页面验收时，应按部署文档设置一次性环境变量后重新启动演示。

## 9. 关键业务用例

| 编号 | 场景 | 预期结果 |
| --- | --- | --- |
| T01 | 四种身份注册或登录 | 返回对应身份令牌，跨身份接口被拒绝 |
| T02 | 商家创建店铺、分类和商品 | 数据归属正确，刷新后仍存在 |
| T03 | 用户维护地址和默认地址 | 只能访问本人地址，最多一个默认地址 |
| T04 | 加购不同店铺商品 | 拒绝跨店混合购物车，原购物车状态一致 |
| T05 | 正常下单 | 服务端计算金额，扣减库存，清理购物车 |
| T06 | 闭店、下架或库存不足时下单 | 请求失败且库存、券和购物车不产生部分修改 |
| T07 | 两个请求争抢最后一件库存 | 仅一单成功，不出现负库存或超卖 |
| T08 | 使用店铺满减券 | 校验归属、门槛、有效期和状态，优惠不超过商品金额 |
| T09 | 取消待处理订单 | 订单取消，库存和优惠券各恢复一次 |
| T10 | 商家确认和骑手配送 | 状态按顺序变化，店铺和骑手归属受保护 |
| T11 | 删除地址或修改商品后查历史订单 | 收货人与商品快照不变 |
| T12 | 网络响应丢失或旧请求晚到 | 页面重新查询服务器状态，不盲目重复写入 |

## 10. 数据库一致性检查

在 MySQL 8.4.9 的 V1～V7 数据库中，最终结构验证得到 14 张表、18 个外键和 45 个索引。金额校验遵循：

```sql
SELECT COUNT(*) AS invalid_order_amount_count
FROM orders
WHERE total_amount <> product_amount - discount_amount + delivery_fee;

SELECT COUNT(*) AS invalid_item_subtotal_count
FROM order_item
WHERE subtotal <> unit_price * quantity;
```

最终两项无效记录数均为 `0`。数据库结构细节见[数据字典](database/data-dictionary.md)和[ER 图](database/er-diagram.md)。

## 11. 人工页面检查

人工检查覆盖：

- 桌面端首页、店铺、商品、购物车、结算、订单和管理员控制台。
- 移动尺寸下的用户页面、商家订单和骑手任务。
- 商品封面和详情缩略图比例、加载失败占位及无横向溢出。
- 商家创建店铺/分类/商品/优惠券、订单确认和营业状态。
- 用户领券、用券、下单、取消及订单详情。
- 骑手接单、查看任务和确认送达。

参考截图位于 `docs/demo-screenshots/`，建议演示顺序见[本地演示指南](demo.md)。自动测试结果不能冒记为人工交叉测试；外部交叉测试由课程安排的 C 角色另行执行并提供报告。

## 12. 已知限制与环境问题

- 项目不接入真实支付，只展示金额和订单结果。
- 一键演示只监听本机，空密码 MySQL 仅用于隔离虚构数据，不能作为生产配置。
- 管理员真实环境检查需要在启动前提供初始化环境变量。
- Windows 同时安装多个 MySQL 版本时，脚本和 Stage 2 必须解析到 MySQL 8.4。
- 本次首次 Stage 2 尝试因第二个终端优先解析到 MySQL 5.5 而失败，并伴随旧客户端错误输出的编码异常；将 MySQL 8.4 的 `bin` 临时置于 PATH 首位后重跑通过。这是本机工具链优先级问题，不是业务测试失败。
- 构建产物的正式静态托管、HTTPS 和反向代理由部署环境提供，仓库当前不包含 Docker、Compose 或 Nginx 配置。

## 13. 测试结论

截至 2026-09-15，后端、前端、生产构建、真实 HTTP、MySQL 事务、并发库存、优惠券闭环、商家履约、骑手配送及权限矩阵均通过既定检查。最终部署可以从当前 `main` 复现，测试和构建未产生受 Git 跟踪的修改。

管理员真实页面在本次最终 Stage 2 中按条件跳过，已有自动测试覆盖；外部交叉测试报告尚待课程交叉测试任务完成后纳入最终实验报告。
