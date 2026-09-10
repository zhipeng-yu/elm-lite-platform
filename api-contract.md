# 接口契约与登录鉴权方案

第 1—8 节记录现有基础接口，第 9 节是第二阶段增量设计，不表示功能已实现。新增接口和测试断言由对应成员开发前复核，实现进度只维护在 `handoff.md`。

## 1. 全局接口契约

- REST 接口统一使用 `/api/v1` 前缀。
- JSON 字段统一使用 `lowerCamelCase`。
- 请求和响应使用 `Content-Type: application/json`。
- 时间使用 ISO 8601，例如 `2026-09-02T12:00:00+08:00`。
- 金额使用整数分，例如 `priceCent: 1590`。
- JSON 整数字段拒绝小数数值并返回 HTTP 400，不允许静默截断后再校验；适用于金额、库存、分类 ID、排序和状态等整数请求字段。
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

`businessStatus` 与数据库一致：`0` 表示休息，`1` 表示营业，`2` 表示临时闭店。接口金额使用整数分，Service 负责与数据库 `DECIMAL` 元金额转换，Entity 不直接作为响应返回。列表直接返回数组，不分页。

### 3.3 商家管理鉴权约定

- 用户和商家使用独立账号，分别保存于 `users` 与 `merchant` 表。
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
| `categoryName` | 分类名必填、非空白，最长 50 个字符；去首尾空白后保存 |
| `sortOrder` | 非负整数，Java `Integer` 范围；创建省略时为 0，与数据库 `INT UNSIGNED` 一致 |
| `productName` | 必填，沿用现有字段名，最长 100 个字符；同店铺允许商品同名 |
| `description` | 可空，最长 255 个字符 |
| `imageUrl` | 可空，最长 255 个字符；只保存 URL，不做图片上传 |
| `priceCent` | 整数分，必须大于 0，最大 `9_999_999_999` |
| `stock` | Java `Integer`，大于等于 0；不额外设业务上限，仍受类型范围约束 |
| `categoryId` | 必须存在且属于当前 `shopId`；新建或修改商品不得挂到停用分类 |
| 分类重名 | 同店铺分类名唯一，由 `uk_category_shop_name` 保证；包括并发创建或修改在内，重复均返回 409 |

保留现有分类唯一约束，不修改已应用的 V1/V2 迁移。

### 6.4 商家管理查询

为管理页面刷新和重新管理停用、下架数据提供以下 GET 接口，全部要求 `MERCHANT` 身份及有效商家账号：

| 路径 | 返回内容 |
| --- | --- |
| `/api/v1/merchant/shops` | 当前商家的店铺数组，按 ID 升序；字段与创建店铺响应一致，包含地址、营业状态和整数分金额，不返回 `merchantId` |
| `/api/v1/merchant/shops/{shopId}/categories` | 本店全部分类，含停用项；按 `sortOrder`、ID 升序；字段与分类写接口响应一致 |
| `/api/v1/merchant/shops/{shopId}/products` | 本店全部商品，含下架项；按 ID 升序；字段与商品写接口响应一致 |

列表不分页，无数据返回 `[]`。账号不存在或禁用返回 403；店铺不存在返回 404、归属其他商家返回 403。公开查询仍只展示原契约允许的数据。商品编辑使用 PATCH，仅发送发生变化的字段，未修改库存时不发送 `stock`。

## 7. 购物车接口

以下为三人确认的购物车方案。全部接口要求 `USER` Token，匿名或无效 Token 返回 401，商家 Token 返回 403；Service 负责当前用户状态及资源归属校验。

| 方法与路径 | 请求与用途 |
| --- | --- |
| `GET /api/v1/cart/items` | 当前用户购物车数组；无商品返回 `[]` |
| `POST /api/v1/cart/items` | 仅传 `productId`、`quantity`，加入购物车 |
| `PATCH /api/v1/cart/items/{id}` | 仅传 `quantity`，替换数量 |
| `DELETE /api/v1/cart/items/{id}` | 删除指定购物车项 |

返回项至少包含 `id`、`productId`、`shopId`、`productName`、`imageUrl`、`priceCent`、`stock`、`status`、`quantity`、`subtotalCent`；不返回 `userId`。

- 同用户重复加入同商品时累加数量，不新增重复记录；累加后仍须校验库存和 Java `Integer` 范围，超出 `Integer.MAX_VALUE` 返回 400，原购物车数据保持不变。
- `quantity` 必须为正整数，非法值返回 400；`PATCH quantity=0` 不代表删除。
- 加入及修改数量时商品必须存在、上架且目标数量不超过实时库存；不存在返回 404，下架或库存不足返回 409。下单再次执行最终库存校验。
- 一个用户购物车只允许同店铺商品；跨店加入返回 409，不自动清空原购物车。
- 下单成功仅删除本次已下单项；库存扣减、订单及明细写入、购物车清理必须处于同一事务，失败全部回滚。

## 8. 订单接口

全部接口要求 `USER` Token。只从 Token 取用户 ID，校验用户存在且未禁用；他人的资源返回 403，不存在返回 404。

| 方法与路径 | 成功响应 |
| --- | --- |
| `POST /api/v1/orders` | HTTP 201，`data` 为新建订单详情 |
| `GET /api/v1/orders` | HTTP 200，`data` 为自己的订单摘要数组，按创建时间和 ID 倒序；空列表 `[]`，不分页 |
| `GET /api/v1/orders/{id}` | HTTP 200，`data` 为订单详情，包含当前状态 |

创建请求示例：

```json
{"addressId": 1, "cartItemIds": [1, 2], "remark": "少辣"}
```

- `addressId` 必填且为正整数；`cartItemIds` 必填、非空、元素为不重复的正整数。ID 使用 Java `Long` 范围；拒绝小数、字符串 ID 和额外字段，非法请求返回 400。
- `remark` 可省略或为 `null`，去首尾空白后最多 255 个字符，空白保存为 `null`。
- 地址和所选购物车项必须属于当前用户；只对所选项下单，不接收客户端价格、数量、用户 ID 或状态。
- 商品必须上架、库存足够且来自同一营业店铺；商品不存在返回 404，跨店、下架、库存不足、店铺休息或临时闭店返回 409。
- 按实时单价计算商品小计及总价，商品金额必须达到店铺起送价，订单总金额为商品金额加配送费；业务冲突返回 409。金额不超过现有 `DECIMAL(10,2)` 上限 `9_999_999_999` 分，不扩展数据库字段。
- 初始 `orderStatus=0`。状态沿用数据库：0 待处理、1 已确认、2 制作中、3 配送中、4 已完成、5 已取消；第二阶段状态动作见第 9 节，不接入真实支付。
- 保存收货信息、商品名、单价、数量、小计和配送费快照；源地址删除或商品信息变更不影响历史订单。
- 订单摘要返回 `id`、`orderNo`、`shopId`、`orderStatus`、`totalAmountCent`、`createdAt`。详情另外返回 `receiverName`、`receiverPhone`、`deliveryAddress`、`productAmountCent`、`deliveryFeeCent`、`remark`、`items`；不返回 `userId`。
- 明细返回 `productId`、`productName`、`unitPriceCent`、`quantity`、`subtotalCent`。`createdAt` 为带 `+08:00` 的 ISO 8601 字符串。

### 8.1 下单内部协作方法

库存和购物车对接集中在 `CheckoutService`，由 `OrderService.create` 的事务调用，不另开事务或提供 HTTP 路径：

| Java 方法 | 契约 |
| --- | --- |
| `Product deductStock(long productId, int quantity)` | 锁定并读取实时商品快照，再以 `status=1 AND stock>=quantity` 条件扣减；数量非法 400，商品不存在 404，下架或扣减失败 409 |
| `void clearItems(long userId, List<Long> cartItemIds)` | 只删除该用户的指定购物车项；删除数量不符返回 409，使整个下单事务回滚 |

两个方法均要求调用方已有事务（`MANDATORY`）；`clearItems` 的调用方须先验证非空且不重复的 ID 列表及归属。余负责订单事务编排；梁复核并复用对接方法，不重复实现库存扣减或购物车清理。购物车 CRUD 仍由梁负责。

下单先锁当前 `users` 行，再锁定并读取选中的购物车项；按商品 ID 升序扣库存，锁定店铺并校验营业状态、起送价与配送费，保存订单及明细后清理选中项。任一步失败全部回滚。地址写操作使用同一用户锁，避免取快照时地址被并发删除；购物车修改必须遵循当前用户及购物车行的相同锁顺序，避免下单期间数量变化或重复处理。重复提交已清理的购物车项返回 404，不承诺跨不同购物车的请求幂等。

商家修改商品只写入请求中明确提供的字段；修改名称、价格或状态时不得将先前读取的库存一并写回，避免覆盖并发订单已扣减的库存。显式提交 `stock` 仍表示设置库存绝对值。

## 9. 第二阶段增量设计

### 9.1 商家订单、取消与库存

下列路径均以 `/api/v1` 开头。

| 方法与路径 | 身份与用途 |
| --- | --- |
| `GET /merchant/shops/{shopId}/orders` | 商家查询本店订单，支持 `orderStatus` 筛选，按创建时间及 ID 倒序 |
| `GET /merchant/orders/{id}` | 商家查看本店订单详情 |
| `POST /merchant/orders/{id}/confirm` | 商家将待处理变为已确认 |
| `POST /merchant/orders/{id}/prepare` | 商家将已确认变为制作中 |
| `POST /orders/{id}/cancel` | 顾客取消自己的待处理订单 |

- 商家列表返回订单号、状态、收货人、商品摘要（名称及数量）、金额和下单时间；详情复用订单商品及收货快照，包含联系电话、地址和备注。下单账号身份与收货人分别标识，不能把收货人姓名当成账号姓名；不提供其他顾客资料。
- 身份和店铺归属由后端校验，不允许商家访问其他店铺订单。创建订单后商家重新查询列表即可看到订单，先使用刷新，不增加推送服务。
- 购物车加减只修改购物车数量；库存只在成功创建订单时扣减。沿用现有扣减和回滚逻辑。
- 取消操作将状态变为 5，并按订单明细数量恢复库存；商品下架不阻止恢复。状态更新、恢复库存和返券必须处于同一事务。
- 重复取消返回已取消订单，不再次恢复库存或券；其他非待处理状态取消返回 409。取消与商家确认并发只能有一个成功，使用统一锁顺序及状态条件更新；库存恢复不得覆盖并发扣减或溢出。
- 查询及状态动作成功返回 200，动作返回更新后的订单；空列表返回 `[]`。订单不存在 404、越权 403、非法状态流转 409。

### 9.2 骑手端

新增 `accountType=RIDER`，骑手独立注册登录；JWT 有效期、密码存储和统一响应沿用已有约定。账号请求字段在实现前按现有商家登录模式补齐并复核。

| 方法与路径 | 用途 |
| --- | --- |
| `POST /riders`、`POST /rider/auth/login` | 注册（201）、登录（200） |
| `GET /rider/available-orders` | 查询制作中且未分配骑手的订单，仅返回订单、店铺及必要取餐信息 |
| `POST /rider/orders/{id}/claim` | 原子领取订单并绑定当前骑手 |
| `GET /rider/orders`、`GET /rider/orders/{id}` | 查询自己的任务及详情，详情包含配送所需收货信息 |
| `POST /rider/orders/{id}/dispatch` | 取餐后将制作中变为配送中 |
| `POST /rider/orders/{id}/complete` | 将配送中变为已完成 |

接单绑定归属但不立即改变制作中状态；只有所属骑手能够取餐和送达。两个骑手抢同一单只允许一个成功，另一请求返回 409；同一骑手重复领取当前已归属自己的订单不重复绑定。重复送达只返回已完成结果，不产生额外变更。管理操作成功返回 200；不存在 404、他人任务 403、状态冲突 409。账号失效时禁止使用旧 Token 继续操作。

### 9.3 商品多图与手机展示

保留 `imageUrl` 作为封面；商品详情、商家商品查询新增有序 `detailImageUrls` 数组，最多 3 项。商家沿用商品创建/修改接口维护数组，PATCH 省略表示不变、空数组表示清空。只接受有效 HTTP(S) 图片地址或本站 `/images/` 路径，不引入上传服务。旧商品没有详情图时返回 `[]`，封面与原有 `description` 继续有效。

顾客页面以手机操作为主：商品大图、详情图、介绍和购买栏均可在 390×844 下使用；首页、店铺、购物车、结算和订单页面需处理加载、空数据、失败提示及底部按钮遮挡。商家与管理员页面兼顾桌面，骑手任务页兼顾手机。

### 9.4 店铺满减券

| 方法与路径 | 用途 |
| --- | --- |
| `GET/POST /merchant/shops/{shopId}/coupons` | 商家查询或创建本店券 |
| `PATCH /merchant/coupons/{id}` | 商家启停本店券，仅修改启停状态 |
| `GET /shops/{shopId}/coupons` | 查询本店当前可领取的券 |
| `POST /coupons/{id}/claims` | 当前顾客领取一次，成功 201、重复领取 409 |
| `GET /coupons/mine` | 查询当前顾客的券及可用/已使用/过期状态 |

- 券字段为 `id`、`shopId`、`name`、`thresholdCent`、`discountCent`、`startsAt`、`expiresAt`、`enabled`；名称必填，门槛非负、优惠大于零，金额沿用整数分上限，起始时间早于到期时间。发行后不修改金额和有效期，避免已领券规则变化。
- 每种券每人领取一次，每单最多一张；有效时间为 `startsAt <= 当前时间 < expiresAt`。下单时后端校验归属、本店、启用、有效期、门槛及未使用状态，不能相信前端金额。
- 下单请求增加可选 `userCouponId`，省略保持原行为；优惠为券面额与商品金额的较小值，不抵配送费。起送价与用券门槛均按优惠前商品金额判断。
- 订单返回保留 `productAmountCent`、`deliveryFeeCent` 和 `totalAmountCent`，新增 `discountAmountCent`；总金额 = 商品金额 − 优惠金额 + 配送费。保存券及优惠金额快照，旧订单优惠为 0；这里的总金额是应付金额，不表示真实支付成功。
- 用券核销、扣库存、写订单和清购物车同事务；一张用户券并发下单只能使用一次，冲突 409。取消返还该用户券，但不延长有效期；已过期或停用的券返还后仍不可用。取消不自动恢复购物车。
- 梁实现核销/返券方法，余负责订单生命周期；对接方法参加调用方事务，统一锁顺序，禁止另开独立事务。方法签名及调用约束见第 9.7 节。

### 9.5 管理员端与权限

新增 `accountType=ADMIN`，管理员账号通过受控初始化创建，无公开注册入口。初始化凭据不得写入仓库或日志。

| 方法与路径 | 用途 |
| --- | --- |
| `POST /admin/auth/login` | 管理员登录 |
| `GET /admin/users`、`GET /admin/merchants` | 查询用户或商家，支持 `keyword`、`status` 筛选 |
| `GET /admin/shops`、`GET /admin/orders` | 查询店铺或订单，支持 `shopId`（订单）、状态等已约定筛选 |
| `GET /admin/orders/{id}` | 查看订单快照与当前状态 |
| `PATCH /admin/users/{id}`、`PATCH /admin/merchants/{id}` | 仅接受 `status: 0/1`，启停账号 |

管理查询成功 200，直接返回数组；普通用户、商家和骑手不能访问管理员接口。只返回管理需要的字段，不返回密码摘要和密钥；列表不批量展示电话与详细住址。用户/商家禁用后旧 Token 的后续受保护请求也必须拒绝；禁用商家停止新订单，保留历史订单及已分配骑手的履约，重新启用才恢复商家处理。商家禁用判断同时补入顾客下单校验。

四种身份按各自路径授权；未登录或无效 Token 为 401，错误身份或资源越权为 403。不能仅靠隐藏前端菜单控制权限。

### 9.6 四种身份鉴权路径汇总

以下均省略 `/api/v1` 前缀；新增身份按各自任务接入，不因本表存在就视为已实现。

| 身份 | 公开注册 / 登录 | 受保护路径 | 资源及账号校验 |
| --- | --- | --- | --- |
| `USER` | `POST /users`、`POST /auth/login` | `/users/me`、`/addresses/**`、`/cart/**`、`/orders/**`、`POST /coupons/{id}/claims`、`GET /coupons/mine` | 当前用户有效；地址、购物车、订单和用户券属于自己 |
| `MERCHANT` | `POST /merchants`、`POST /merchant/auth/login` | `/merchant/**`（登录除外） | 当前商家有效；店铺、商品、订单及券属于其店铺 |
| `RIDER` | `POST /riders`、`POST /rider/auth/login` | `/rider/**`（登录除外） | 当前骑手有效；接单前校验可领取，接单后校验任务归属 |
| `ADMIN` | 仅 `POST /admin/auth/login`，无公开注册 | `/admin/**`（登录除外） | 当前管理员有效；仅开放第 9.5 节列出的管理能力 |

公开店铺、商品和可领取券查询不要求登录。Security 的公开例外应先于身份路径规则；Service 仍须检查账号状态及资源归属。账号 ID 可能在四张表中相同，必须同时依据 `accountType` 和 `sub` 判断，不能只比较 ID。

### 9.7 商家查询字段与用券对接

商家查询及用券对接采用以下约定，不改变第 9.1、9.2、9.4 节的业务范围。接口契约不表示相应功能已实现。

- 商家订单列表沿用订单摘要字段，增加 `receiverName`、`items`（商品名称及数量），不批量返回电话和详细地址；不分页。可选 `orderStatus` 只接受整数 0—5，非法值返回 400；省略表示全部状态。
- 商家详情沿用顾客详情的平铺字段，增加 `buyer: { id, displayName }`，区分下单账号与收货人。`displayName` 读取账号当前昵称，不伪称下单时昵称快照；不暴露账号登录名、电话或密码摘要。商品和收货信息始终读取订单快照。
- 取消、确认与骑手动作沿用第 9.1、9.2 节：`0→1→2→3→4`，顾客仅可 `0→5`；接单只绑定骑手，状态仍为 2；重复取消、本人重复接单和重复送达不重复产生副作用。

梁在优惠券 Service 提供以下内部方法，余在订单生命周期中调用；不为内部方法增加 HTTP 接口：

| Java 方法 | 调用点及行为 |
| --- | --- |
| `CouponUsageResult consumeCoupon(long userId, long userCouponId, long shopId, long productAmountCent)` | 订单事务锁定商品、按实时单价计算金额并校验店铺后调用，核验并核销用户券，返回用户券 ID、原始券 ID 和实际优惠整数分；订单模块保存券关联及优惠快照。未选券不调用，优惠为 0 |
| `void returnCoupon(long userId, long userCouponId)` | 首次取消时，从已锁定且属于当前用户的订单读取用户券 ID，恢复库存后调用；校验券归属，仅允许已使用券恢复为未使用，不延长有效期。未用券不调用 |

核销返回类型为 `CouponUsageResult(long userCouponId, long couponId, long discountAmountCent)`。订单至少保存 `user_coupon_id` 与实际优惠 `discount_amount`，旧订单优惠为 0；金额展示按第 9.4 节转换为整数分，不扩展券名称、门槛和面额等展示快照。

两个方法均使用 `MANDATORY`，脱离调用方事务时失败，不得另开事务；任一步失败必须回滚订单、库存、购物车和券。核销必须核验第 9.4 节全部条件，关联错误不得默默当作未用券。

下单锁顺序为“用户→所选购物车→商品 ID 升序→店铺→用户券”，在实时金额确定后核销，再写入新订单、明细和优惠快照，最后清理购物车。用户券不提前到商品锁之前；券校验失败时，同一事务回滚此前的库存扣减。

取消锁顺序为“用户→已有订单→商品 ID 升序→用户券”。订单模块先检查归属及状态，仅首次 `0→5` 才恢复库存、返券；已取消订单直接返回，不再调用 `returnCoupon`。用户券 ID 必须取自订单保存的关联，不能接受客户端指定，这也避免旧订单重复取消时返还后来被新订单使用的券。商家确认及骑手状态动作只锁目标订单并作条件更新。

核销和返券均先通过 `SELECT ... FOR UPDATE` 锁定用户券，再校验归属和状态，以包含 `id`、`user_id`、原状态的条件更新执行 `UNUSED→USED` 或 `USED→UNUSED`；影响行数不为 1 时返回 409 并回滚。券模板启停、领取和核销不得在持有券锁后反向请求用户、已有订单或商品锁。返还不修改有效期，过期或停用的券返还后仍不可用。

并发核销、取消与确认竞争、取消后重新用券再重复取消旧订单、失败回滚和真实 MySQL 锁行为，须在对应开发日用测试验证。

## 10. 范围边界

不增加真实支付、地图定位、自动派单、配送算法、图片上传、券叠加、退款投诉或复杂统计。沿用单个 Access Token，不增加 Refresh Token、验证码、第三方登录和 Token 黑名单。分工及 TDD 流程见 `CONTRIBUTING.md`；测试和联调证据写对应 PR。
