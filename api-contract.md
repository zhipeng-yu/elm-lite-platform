# 接口契约与登录鉴权方案

- 状态：三名成员已确认
- 确认日期：2026-09-02

## 1. 全局接口契约

- REST 接口统一使用 `/api/v1` 前缀。
- JSON 字段统一使用 `lowerCamelCase`。
- 请求和响应使用 `Content-Type: application/json`。
- 时间使用 ISO 8601，例如 `2026-09-02T12:00:00+08:00`。
- 金额使用整数分，例如 `priceCent: 1590`。
- 地址、购物车和订单等用户资源不接收客户端传入的 `userId`，后端从登录凭据取得当前用户。

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

### 3.3 尚待确认的商家管理接口

商家注册、开店和营业状态修改涉及独立商家账号的鉴权方式，现有用户 JWT 契约不能直接复用。相关路径和鉴权方式由老师或小组确认后再冻结；确认前不得用客户端传入的 `merchantId` 代替身份校验。

## 4. 登录鉴权方案

- 使用单个 JWT Access Token，不使用 Refresh Token。
- Token 通过 `Authorization: Bearer <accessToken>` 请求头发送。
- JWT 只保存 `iss`、`sub`、`iat` 和 `exp`；`iss` 固定为 `elm-lite-platform`，`sub` 保存用户 ID。
- Token 有效期为 1 小时；过期后重新登录。
- JWT 密钥从 `JWT_SECRET` 环境变量读取，禁止写入仓库。
- 后端使用 Spring Security 的 JWT 支持校验签名和过期时间，依赖版本由 Spring Boot 管理。
- 密码使用 BCrypt 保存，不得保存或比较明文密码。
- 前端将 Token 保存到 `sessionStorage`，由 Axios 拦截器统一添加认证头。
- 退出登录只删除前端 Token，暂不增加退出接口或 Token 黑名单。
- 注册和登录是公开接口；`/users/me`、地址、购物车和订单接口必须登录。
- 服务层根据 JWT 中的当前用户 ID 校验资源归属；登录但操作他人资源时返回 HTTP `403`。

## 5. 明确不做

当前基础范围不实现 Refresh Token、多设备会话、验证码、第三方登录、管理员权限和 Token 黑名单。需求明确增加时再单独讨论。

## 6. 分工与 TDD 验收

- 余（A）：统一响应、异常处理、Spring Security/JWT、注册登录和鉴权测试及实现。
- 梁（B）：确认用户和认证所需数据库字段，并负责版本化数据库迁移。
- 龙（C）：Axios 认证头、Token 保存、统一错误处理和前端登录状态。

实现这些接口时必须保留测试提交早于实现提交的记录，至少覆盖注册成功、用户名冲突、登录成功、错误凭据、缺少 Token、无效 Token、过期 Token 和越权访问。
