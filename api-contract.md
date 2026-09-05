# 接口契约与登录鉴权方案

- 状态：三名成员已确认
- 确认日期：2026-09-02；商家身份方案补充确认：2026-09-04；地址、分类与商品方案补充确认：2026-09-05

## 1. 全局接口契约

- REST 接口统一使用 `/api/v1` 前缀。
- JSON 字段统一使用 `lowerCamelCase`。
- 请求和响应使用 `Content-Type: application/json`。
- 时间使用 ISO 8601，例如 `2026-09-02T12:00:00+08:00`。
- 金额使用整数分，例如 `priceCent: 1590`。
- 地址、购物车和订单等用户资源不接收客户端传入的 `userId`，后端从登录凭据取得当前用户；创建店铺同样不接收 `merchantId`。

成功响应统一为：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

失败响应统一为：

```json
{
  "code": 400,
  "msg": "参数校验失败",
  "data": {
    "fieldErrors": {
      "password": "长度至少为 8 位"
    }
  }
}
```

`msg` 用于展示，前端业务判断使用 `code`。失败时 `data` 默认是 `null`，只有参数校验失败时返回 `fieldErrors`。

| HTTP 状态 | `code` | 含义 |
| ---: | ---: | --- |
| 400 | 400 | 参数错误 |
| 401 | 401 | 未登录、Token 无效或过期 |
| 403 | 403 | 已登录但无权操作 |
| 404 | 404 | 数据不存在 |
| 409 | 409 | 用户名重复或业务状态冲突 |
| 500 | 500 | 服务器内部异常 |

## 2. 最小用户接口

### 2.1 注册

```http
POST /api/v1/users
```

```json
{
  "username": "yuzhi",
  "password": "12345678",
  "displayName": "余"
}
```

成功时返回 HTTP `201`：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "yuzhi",
    "displayName": "余"
  }
}
```

密码不得出现在响应、日志或异常信息中；用户名重复返回 HTTP `409`。

### 2.2 登录

```http
POST /api/v1/auth/login
```

```json
{
  "username": "yuzhi",
  "password": "12345678"
}
```

成功时返回 HTTP `200`：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "accessToken": "<JWT>",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "yuzhi",
      "displayName": "余"
    }
  }
}
```

账号不存在和密码错误统一返回 HTTP `401`，响应信息统一为“账号或密码错误”。

### 2.3 当前用户

```http
GET /api/v1/users/me
PATCH /api/v1/users/me
```

两个接口都必须登录。`PATCH` 当前只更新 `displayName`，不允许通过此接口修改用户名或密码。

## 3. 公开店铺接口

### 3.1 店铺列表

```http
GET /api/v1/shops
```

无需登录。成功时返回 HTTP `200`：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "shopName": "校园美食店",
      "description": "用于本地开发和测试的演示店铺",
      "imageUrl": null,
      "startPriceCent": 1500,
      "deliveryPriceCent": 300,
      "businessStatus": 1
    }
  ]
}
```

### 3.2 店铺详情

```http
GET /api/v1/shops/{id}
```

无需登录。返回字段与列表项一致，并增加 `address`；店铺不存在时返回 HTTP `404`。

`businessStatus` 与数据库一致：`0` 表示休息，`1` 表示营业，`2` 表示临时闭店。接口金额继续使用整数分，Service 负责与数据库 `DECIMAL` 元金额转换，Entity 不直接作为响应返回。D3 基础范围直接返回数组，暂不增加分页。

### 3.3 商家管理鉴权约定

- 用户和商家使用独立账号，继续使用现有 `users` 与 `merchant` 表结构，不修改 D2 Entity 和数据库。
- 商家使用独立登录入口，登录成功后签发 `accountType=MERCHANT` 的 Token。
- 创建店铺时，后端使用 Token 的 `sub` 作为当前商家 ID 并写入 `shop.merchant_id`，前端不传 `merchantId`。
- 修改 `/api/v1/merchant/shops/{id}` 的营业状态时，Service 必须校验店铺属于当前商家，否则返回 HTTP `403`。
- `GET /api/v1/shops` 和 `GET /api/v1/shops/{id}` 始终无需登录。

## 4. 登录鉴权方案

- 使用单个 JWT Access Token，不使用 Refresh Token。
- Token 通过 `Authorization: Bearer <accessToken>` 请求头发送。
- JWT 保存 `iss`、`sub`、`iat`、`exp` 和 `accountType`；`iss` 固定为 `elm-lite-platform`，`sub` 保存对应账号表的 ID。
- 用户登录签发 `accountType=USER` 的 Token，商家登录签发 `accountType=MERCHANT` 的 Token；受保护接口按账号类型授权。
- Token 有效期为 1 小时；过期后重新登录。
- JWT 密钥从 `JWT_SECRET` 环境变量读取，禁止写入仓库。
- 后端使用 Spring Security 的 JWT 支持校验签名和过期时间，依赖版本由 Spring Boot 管理。
- 密码使用 BCrypt 保存，不得保存或比较明文密码。
- 前端将 Token 保存到 `sessionStorage`，由 Axios 拦截器统一添加认证头。
- 退出登录只删除前端 Token，暂不增加退出接口或 Token 黑名单。
- 用户注册、用户登录和商家登录是公开接口；`/users/me`、地址、购物车、订单及商家管理接口必须使用对应身份登录。
- 服务层根据 JWT 中的当前账号 ID 校验资源归属；登录但操作他人资源时返回 HTTP `403`。

## 5. 收货地址接口

### 5.1 接口与返回字段

全部接口必须使用 `accountType=USER` 的 Token。`userId` 仅取自 Token，不接受客户端指定归属。

| 方法与路径 | 成功响应 |
| --- | --- |
| `GET /api/v1/addresses` | HTTP 200，`data` 为当前用户地址数组；无地址返回 `[]`，不分页 |
| `GET /api/v1/addresses/{id}` | HTTP 200，`data` 为地址对象，供编辑页回填 |
| `POST /api/v1/addresses` | HTTP 201，`data` 为新增地址对象 |
| `PATCH /api/v1/addresses/{id}` | HTTP 200，`data` 为修改后的完整地址对象 |
| `DELETE /api/v1/addresses/{id}` | HTTP 200，`code=0`、`msg=success`、`data=null` |

地址对象统一返回 `id`、`receiverName`、`receiverPhone`、`addressDetail`、`addressLabel`、`isDefault`；不返回 `userId`、创建时间或修改时间。列表默认地址在前，其余按 `id` 降序。

### 5.2 请求字段与修改语义

| 字段 | 新增及校验规则 |
| --- | --- |
| `receiverName` | 必填、非空白，最长 50 个字符 |
| `receiverPhone` | 必填字符串，按 `^1[0-9]{10}$` 校验；不做运营商号段校验，不支持座机或国际号码 |
| `addressDetail` | 必填、非空白，最长 255 个字符；使用单一文本字段 |
| `addressLabel` | 可选，最长 20 个字符，允许自定义；空字符串或 `null` 保存为 `null` |
| `isDefault` | 整数 `0` 或 `1`；新增省略时为 `0`；显式 `null` 或其他数值返回 400 |

- 姓名、电话、详细地址和标签先去首尾空白再校验，不修改内部文字。
- `PATCH` 省略字段保持原值；必填业务字段提交 `null` 或空白返回 400；`addressLabel` 提交 `null` 或空字符串表示清空；空对象返回 400。
- 请求仅接受上述可写字段；传入 `userId`、`id`、`createdAt`、`updatedAt` 或其他额外字段返回 400，在地址 DTO 内限制，不改变全局 JSON 配置。
- 不拆省市区，不接地图或配送计算，不增加地址数量上限。

### 5.3 默认地址及删除

- 同一用户最多一个默认地址，允许没有默认地址；首条地址不自动设为默认。
- 新增或通过 `PATCH` 设置 `isDefault=1` 时，同一事务中取消该用户旧默认，再设置新默认；不另增设置默认的动作接口。
- `isDefault=0` 可取消默认。删除默认地址后不自动选择其他地址，前端提示用户重新选择。
- 同一用户的新增、修改和删除在 Service 事务内锁定对应 `users` 行，再处理默认状态，避免并发产生多个默认地址；不引入锁库或缓存。
- 地址允许物理删除，作为历史关联数据不物理删除约定的明确例外。沿用现有 `orders.address_id` 的 `ON DELETE SET NULL`，订单收货人、联系电话、详细地址快照不变；不新增软删除字段。
- 首次删除成功，再次删除同一 ID 返回 404。

### 5.4 权限与验收

- 匿名、无效或过期 Token 返回 401；商家 Token 返回 403。用户禁用后即使持有有效 Token 也返回 403；当前用户记录不存在沿用用户模块的 404。
- 列表按当前用户筛选；详情、修改和删除先查资源，存在但归属他人返回 403，不存在返回 404。
- 非法路径 ID、请求格式和字段校验错误返回 400；字段校验返回 `data.fieldErrors`，其余错误沿用全局响应。
- 测试覆盖增删改查、默认切换、空列表、权限、空白/`null`、长度边界、非法默认值、重复删除及失败后数据不变；验证事务回滚、并发默认唯一和删除后订单快照不变，并在真实 MySQL 复验并发与外键行为。
- 前端提供加载、空列表、字段错误和提交失败提示；提交中禁用重复点击，删除前确认，成功后重新拉取列表。

## 6. 分类与商品接口

### 6.1 公开查询

下列 GET 无需登录。列表不分页，`data` 直接返回数组。

| 路径 | 返回字段或筛选 |
| --- | --- |
| `/api/v1/shops/{shopId}/categories` | `id`、`categoryName`、`sortOrder` |
| `/api/v1/shops/{shopId}/products` | `id`、`categoryId`、`productName`、`description`、`imageUrl`、`priceCent`、`stock`、`status`；支持 `?categoryId={id}` |
| `/api/v1/products/{id}` | `id`、`shopId`、`categoryId`、`categoryName`、`productName`、`description`、`imageUrl`、`priceCent`、`stock`、`status`；不存在返回 404 |

商品列表不返回 `categoryName`，详情返回。金额使用整数分，由 Service 与数据库 `DECIMAL` 元金额转换，不直接返回 Entity。

- 分类状态 `0=停用`、`1=启用`；公开分类列表仅返回 `status=1`。
- 商品状态 `0=下架`、`1=上架`；公开商品列表仅返回 `status=1`，下架商品详情不对外提供。
- 库存为 0 可保持上架，前端显示“售罄”。店铺休息或临时闭店时商品仍可查询，但不允许下单；下单约束由订单阶段实现。

### 6.2 商家管理

| 方法与路径 | 用途 |
| --- | --- |
| `POST /api/v1/merchant/shops/{shopId}/categories` | 新增分类 |
| `POST /api/v1/merchant/shops/{shopId}/products` | 新增商品 |
| `PATCH /api/v1/merchant/categories/{id}` | 修改分类 |
| `PATCH /api/v1/merchant/products/{id}` | 修改商品 |

- 必须使用 `MERCHANT` Token，并由 Service 校验店铺、分类和商品归属；操作其他商家资源返回 403，资源不存在返回 404。
- 分类和商品创建时由后端默认设置 `status=1`，不要求前端传入。
- 不提供分类和商品 DELETE；分类通过 `status` 停用，商品通过 `status` 下架。

### 6.3 字段与业务边界

| 字段或规则 | 约定 |
| --- | --- |
| `productName` | 必填，沿用现有字段名，最长 100 个字符；同店铺允许商品同名 |
| `description` | 可空，最长 255 个字符 |
| `imageUrl` | 可空，最长 255 个字符；只保存 URL，不做图片上传 |
| `priceCent` | 整数分，必须大于 0，最大 `9_999_999_999` |
| `stock` | Java `Integer`，大于等于 0；不额外设业务上限，仍受类型范围约束 |
| `categoryId` | 必须存在且属于当前 `shopId`；新建或修改商品不得挂到停用分类 |
| 分类重名 | 同店铺分类名唯一，沿用 `uk_category_shop_name`，重复返回 409 |

分类重名采用余清单中的最终修正，替代梁清单早期“同店铺分类可重名”的建议；不为此修改 V1/V2 或移除现有唯一约束。

## 7. 明确不做

当前基础范围不实现 Refresh Token、多设备会话、验证码、第三方登录、管理员权限和 Token 黑名单。需求明确增加时再单独讨论。

## 8. 分工与 TDD 验收

- 余（A）：统一响应、异常处理、Spring Security/JWT、注册登录、收货地址及相关鉴权测试与实现；汇总接口契约。
- 梁（B）：商家注册、独立登录、店铺、分类和商品管理与商家资源归属校验；负责必要的版本化迁移、Entity/Mapper 和测试数据。
- 龙（C）：Axios 认证头、Token 保存、统一错误处理、前端登录状态及地址、分类、商品页面与联调。

公共鉴权由余统一维护并与梁审查：地址路径及子路径要求 `USER`；商家分类和商品路径要求 `MERCHANT`，不能只依赖 `authenticated`；公开查询仅放行对应 GET，尤其 `/api/v1/products/{id}`，不放行写操作。梁继续负责 Service 归属校验。共享接口契约由余汇总，梁、龙提供确认内容，避免同时修改。

实现这些接口时必须保留测试提交早于实现提交的记录，至少覆盖注册成功、用户名冲突、登录成功、错误凭据、缺少 Token、无效 Token、过期 Token 和越权访问。
