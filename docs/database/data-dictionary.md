# 数据库字段说明

## 1. 基本信息

* 数据库：MySQL 8.4；下列字段对应 V1—V7 迁移，共 14 张表
* 字符集：utf8mb4
* 存储引擎：InnoDB
* 主键：BIGINT 自增
* 金额：DECIMAL(10,2)
* 状态：TINYINT
* 创建时间：通常为 TIMESTAMP；`admin_account` 使用 DATETIME
* 修改时间：DATETIME

## 2. users 用户表

| 字段            | 类型           | 约束       | 说明        |
| ------------- | ------------ | -------- | --------- |
| id            | BIGINT       | PK、AI、NN | 用户编号      |
| username      | VARCHAR(50)  | UQ、NN    | 登录用户名（V2 新增） |
| phone         | VARCHAR(20)  | UQ、可空  | 手机号（V2 调整为可空） |
| password_hash | VARCHAR(100) | NN       | 加密后的密码    |
| nickname      | VARCHAR(50)  | NN       | 用户昵称      |
| gender        | TINYINT      | NN、默认0   | 0未知，1男，2女 |
| avatar_url    | VARCHAR(255) | 可空       | 头像地址      |
| status        | TINYINT      | NN、默认1   | 0禁用，1正常   |
| created_at    | TIMESTAMP    | NN、自动生成  | 创建时间      |
| updated_at    | DATETIME     | 可空       | 最后修改时间    |

## 3. merchant 商家表

| 字段            | 类型           | 约束       | 说明      |
| ------------- | ------------ | -------- | ------- |
| id            | BIGINT       | PK、AI、NN | 商家编号    |
| account       | VARCHAR(50)  | UQ、NN    | 商家登录账号  |
| password_hash | VARCHAR(100) | NN       | 加密后的密码  |
| merchant_name | VARCHAR(100) | NN       | 商家名称    |
| contact_name  | VARCHAR(50)  | NN       | 联系人姓名   |
| contact_phone | VARCHAR(20)  | NN       | 联系人电话   |
| status        | TINYINT      | NN、默认1   | 0禁用，1正常 |
| created_at    | TIMESTAMP    | NN、自动生成  | 创建时间    |
| updated_at    | DATETIME     | 可空       | 最后修改时间  |

## 4. shop 店铺表

| 字段              | 类型            | 约束        | 说明            |
| --------------- | ------------- | --------- | ------------- |
| id              | BIGINT        | PK、AI、NN  | 店铺编号          |
| merchant_id     | BIGINT        | FK、NN     | 所属商家编号        |
| shop_name       | VARCHAR(100)  | NN        | 店铺名称          |
| description     | VARCHAR(255)  | 可空        | 店铺简介          |
| address         | VARCHAR(255)  | NN        | 店铺地址          |
| image_url       | VARCHAR(255)  | 可空        | 店铺图片地址        |
| start_price     | DECIMAL(10,2) | NN、默认0.00 | 起送价格          |
| delivery_price  | DECIMAL(10,2) | NN、默认0.00 | 配送价格          |
| business_status | TINYINT       | NN、默认0    | 0休息，1营业，2临时闭店 |
| created_at      | TIMESTAMP     | NN、自动生成   | 创建时间          |
| updated_at      | DATETIME      | 可空        | 最后修改时间        |

## 5. product_category 商品分类表

| 字段            | 类型           | 约束       | 说明             |
| ------------- | ------------ | -------- | -------------- |
| id            | BIGINT       | PK、AI、NN | 分类编号           |
| shop_id       | BIGINT       | FK、NN    | 所属店铺编号         |
| category_name | VARCHAR(50)  | NN       | 分类名称，同一店铺内不可重复 |
| sort_order    | INT UNSIGNED | NN、默认0   | 显示顺序；接口接受非负 Java Integer |
| status        | TINYINT      | NN、默认1   | 0停用，1启用        |
| created_at    | TIMESTAMP    | NN、自动生成  | 创建时间           |
| updated_at    | DATETIME     | 可空       | 最后修改时间         |

## 6. product 商品表

| 字段           | 类型                     | 约束       | 说明      |
| ------------ | ---------------------- | -------- | ------- |
| id           | BIGINT                 | PK、AI、NN | 商品编号    |
| shop_id      | BIGINT                 | FK、NN    | 所属店铺编号  |
| category_id  | BIGINT                 | FK、NN    | 所属分类编号  |
| product_name | VARCHAR(100)           | NN       | 商品名称    |
| description  | VARCHAR(255)           | 可空       | 商品介绍    |
| image_url    | VARCHAR(255)           | 可空       | 商品图片地址  |
| price        | DECIMAL(10,2) UNSIGNED | NN       | 商品价格    |
| stock        | INT UNSIGNED           | NN、默认0   | 库存数量    |
| status       | TINYINT                | NN、默认1   | 0下架，1上架 |
| created_at   | TIMESTAMP              | NN、自动生成  | 创建时间    |
| updated_at   | DATETIME               | 可空       | 最后修改时间  |

## 7. product_detail_image 商品详情图片表

| 字段       | 类型           | 约束        | 说明       |
| ---------- | -------------- | ----------- | ---------- |
| id         | BIGINT         | PK、AI、NN  | 详情图编号 |
| product_id | BIGINT         | FK、NN      | 所属商品编号 |
| image_url  | VARCHAR(255)   | NN          | 详情图片地址 |
| sort_order | INT UNSIGNED   | NN、默认0   | 显示顺序   |

`product_id` 使用索引 `idx_product_detail_image_product_id`；外键 `fk_product_detail_image_product` 指向 `product(id)`，删除商品时按 `ON DELETE CASCADE` 删除详情图。接口最多 3 张是 Service 约束，不是数据库行数约束；只接受有效 HTTP(S) 或本站 `/images/` 路径。

## 8. cart_item 购物车明细表

| 字段         | 类型           | 约束       | 说明      |
| ---------- | ------------ | -------- | ------- |
| id         | BIGINT       | PK、AI、NN | 购物车明细编号 |
| user_id    | BIGINT       | FK、NN    | 所属用户编号  |
| product_id | BIGINT       | FK、NN    | 商品编号    |
| quantity   | INT UNSIGNED | NN、默认1   | 商品数量    |
| created_at | TIMESTAMP    | NN、自动生成  | 创建时间    |
| updated_at | DATETIME     | 可空       | 最后修改时间  |

同一用户与同一商品只能存在一条购物车记录。业务数量必须为正整数；重复加入累加数量，一个用户购物车只允许同店铺商品。下单仅清理选中的购物车项。

## 9. delivery_address 收货地址表

| 字段             | 类型           | 约束       | 说明         |
| -------------- | ------------ | -------- | ---------- |
| id             | BIGINT       | PK、AI、NN | 收货地址编号     |
| user_id        | BIGINT       | FK、NN    | 所属用户编号     |
| receiver_name  | VARCHAR(50)  | NN       | 收货人姓名      |
| receiver_phone | VARCHAR(20)  | NN       | 收货人联系电话    |
| address_detail | VARCHAR(255) | NN       | 详细收货地址     |
| address_label  | VARCHAR(20)  | 可空       | 家、学校、公司等标签 |
| is_default     | TINYINT      | NN、默认0   | 0非默认，1默认   |
| created_at     | TIMESTAMP    | NN、自动生成  | 创建时间       |
| updated_at     | DATETIME     | 可空       | 最后修改时间     |

同一用户最多一个默认收货地址，允许没有默认地址；首条地址不自动设为默认。新增、修改或删除地址时，由 Service 在事务内锁定对应 `users` 行并维护默认状态；取消或删除默认地址不自动指定其他地址。

地址允许物理删除；关联订单的 `address_id` 按现有外键 `ON DELETE SET NULL` 清空，订单收货信息快照不变，不新增软删除字段。接口联系电话按 1 开头的 11 位数字校验；数据库仍保留现有 `VARCHAR(20)` 字段，不因接口校验规则变更表结构。

## 10. orders 订单表

| 字段               | 类型                     | 约束        | 说明         |
| ---------------- | ---------------------- | --------- | ---------- |
| id               | BIGINT                 | PK、AI、NN  | 订单编号       |
| order_no         | VARCHAR(32)            | UQ、NN     | 对外展示的订单号   |
| user_id          | BIGINT                 | FK、NN     | 下单用户编号     |
| shop_id          | BIGINT                 | FK、NN     | 所属店铺编号     |
| rider_id         | BIGINT                 | FK、可空   | 领取订单的骑手编号 |
| address_id       | BIGINT                 | FK、可空     | 下单时选择的地址编号 |
| user_coupon_id   | BIGINT                 | FK、可空     | 本订单使用的用户优惠券编号 |
| receiver_name    | VARCHAR(50)            | NN        | 收货人姓名快照    |
| receiver_phone   | VARCHAR(20)            | NN        | 收货人电话快照    |
| delivery_address | VARCHAR(255)           | NN        | 收货地址快照     |
| product_amount   | DECIMAL(10,2) UNSIGNED | NN、默认0.00 | 商品总金额      |
| discount_amount  | DECIMAL(10,2) UNSIGNED | NN、默认0.00 | 实际优惠金额快照 |
| delivery_fee     | DECIMAL(10,2) UNSIGNED | NN、默认0.00 | 配送费        |
| total_amount     | DECIMAL(10,2) UNSIGNED | NN、默认0.00 | 订单总金额      |
| order_status     | TINYINT                | NN、默认0    | 订单状态       |
| remark           | VARCHAR(255)           | 可空        | 订单备注       |
| created_at       | TIMESTAMP              | NN、自动生成   | 创建时间       |
| updated_at       | DATETIME               | 可空        | 最后修改时间     |

V7 新增 `idx_orders_user_coupon_id(user_coupon_id)`，`discount_amount` 非负、默认 0，旧订单无优惠。用户券关联删除时置空，但实际优惠金额快照不变；当前没有删除用户券的公开接口。

订单状态：

| 状态值 | 含义  |
| --- | --- |
| 0   | 待处理 |
| 1   | 已确认 |
| 2   | 制作中 |
| 3   | 配送中 |
| 4   | 已完成 |
| 5   | 已取消 |

## 11. order_item 订单明细表

| 字段           | 类型                     | 约束       | 说明         |
| ------------ | ---------------------- | -------- | ---------- |
| id           | BIGINT                 | PK、AI、NN | 订单明细编号     |
| order_id     | BIGINT                 | FK、NN    | 所属订单编号     |
| product_id   | BIGINT                 | FK、NN    | 商品编号       |
| product_name | VARCHAR(100)           | NN       | 下单时的商品名称快照 |
| unit_price   | DECIMAL(10,2) UNSIGNED | NN       | 下单时的商品单价   |
| quantity     | INT UNSIGNED           | NN       | 购买数量       |
| subtotal     | DECIMAL(10,2) UNSIGNED | NN       | 商品小计       |

同一订单中的同一商品只保存一条订单明细。

## 12. admin_account 管理员账号表

| 字段          | 类型           | 约束        | 说明             |
| ------------- | -------------- | ----------- | ---------------- |
| id            | BIGINT         | PK、AI、NN  | 管理员编号       |
| username      | VARCHAR(50)    | UQ、NN      | 管理员登录名     |
| password_hash | VARCHAR(100)   | NN          | BCrypt 密码摘要  |
| status        | TINYINT        | NN、默认1   | 0停用，1启用     |
| created_at    | DATETIME       | NN、自动生成 | 创建时间         |
| updated_at    | DATETIME       | 可空        | 修改时间         |

管理员表不关联其他业务表；用户名使用唯一索引 `uk_admin_account_username`。账号没有公开注册入口，仅在启动时通过 `ADMIN_USERNAME`、`ADMIN_PASSWORD` 环境变量受控初始化，日志和仓库不记录明文密码。

## 13. coupon 优惠券表

| 字段             | 类型                     | 约束        | 说明             |
| ---------------- | ------------------------ | ----------- | ---------------- |
| id               | BIGINT                   | PK、AI、NN  | 优惠券编号       |
| shop_id          | BIGINT                   | FK、NN      | 所属店铺编号     |
| name             | VARCHAR(255)             | NN          | 优惠券名称       |
| threshold_amount | DECIMAL(10,2) UNSIGNED   | NN          | 使用门槛金额     |
| discount_amount  | DECIMAL(10,2) UNSIGNED   | NN          | 优惠金额         |
| starts_at        | DATETIME                 | NN          | 生效时间         |
| expires_at       | DATETIME                 | NN          | 到期时间         |
| enabled          | TINYINT                  | NN、默认1   | 0停用，1启用     |
| created_at       | TIMESTAMP                | NN、自动生成 | 创建时间         |
| updated_at       | DATETIME                 | 可空        | 最后修改时间     |

`shop_id` 指向 `shop(id)`；查询索引为 `idx_coupon_shop_id` 和联合索引 `idx_coupon_shop_enabled_time(shop_id, enabled, starts_at, expires_at)`。金额和有效期发行后不可修改；有效时间为 `starts_at <= 当前时间 < expires_at`。

## 14. user_coupon 用户优惠券表

| 字段       | 类型       | 约束        | 说明             |
| ---------- | ---------- | ----------- | ---------------- |
| id         | BIGINT     | PK、AI、NN  | 用户优惠券编号   |
| user_id    | BIGINT     | FK、NN      | 用户编号         |
| coupon_id  | BIGINT     | FK、NN      | 优惠券编号       |
| status     | TINYINT    | NN、默认0   | 0未使用，1已使用 |
| created_at | TIMESTAMP  | NN、自动生成 | 领取时间         |
| updated_at | DATETIME   | 可空        | 最后修改时间     |

`user_id`、`coupon_id` 分别指向 `users(id)`、`coupon(id)`；唯一索引 `uk_user_coupon_user_coupon(user_id, coupon_id)` 保证同一用户不能重复领取同一优惠券。查询索引为 `idx_user_coupon_user_status(user_id, status)` 和 `idx_user_coupon_coupon_id(coupon_id)`。

过期、停用是结合券模板计算的展示状态，不另存入 `user_coupon.status`；返券只改使用状态，不延长有效期。

## 15. rider 骑手表

| 字段          | 类型           | 约束        | 说明         |
| ------------- | -------------- | ----------- | ------------ |
| id            | BIGINT         | PK、AI、NN  | 骑手编号     |
| username      | VARCHAR(50)    | UQ、NN      | 骑手登录账号 |
| password_hash | VARCHAR(100)   | NN          | 加密后的密码 |
| display_name  | VARCHAR(50)    | NN          | 骑手姓名     |
| status        | TINYINT        | NN、默认1   | 0禁用，1正常 |
| created_at    | TIMESTAMP      | NN、自动生成 | 创建时间     |
| updated_at    | DATETIME       | 可空        | 最后修改时间 |

骑手用户名使用唯一索引 `uk_rider_username`。`orders.rider_id` 可空并指向 `rider(id)`；联合索引 `idx_orders_rider_status(rider_id, order_status)` 支持按骑手和状态查询任务。

## 16. 业务约束

1. 商品接口价格必须大于零，最大为 `9_999_999_999` 分；商品库存不能为负数，使用 Java `Integer` 范围，不额外设业务上限。购物车数量必须为正整数。
2. 商品所属分类必须属于商品对应的店铺；新建或修改商品不得挂到停用分类。
3. 一个订单只能包含同一家店铺的商品。
4. 订单必须至少包含一条订单明细。
5. 商品小计等于商品单价乘以购买数量。
6. 订单总金额等于商品总金额减实际优惠金额再加配送费；优惠券不抵扣配送费。
7. 下单数量不能超过商品库存。
8. 订单保存商品和地址快照，避免源数据修改后影响历史订单。
9. 同店铺商品可同名，分类名保持唯一，沿用 `uk_category_shop_name`；Service 将创建或修改时的唯一键冲突转换为 409，包括并发重名。
10. 分类、商品创建时默认状态为 1；分类停用和商品下架均修改状态，不提供物理删除接口。库存为 0 可以保持上架。
11. 下单时校验店铺营业、商品上架、实时库存和起送价；库存条件扣减、订单及明细写入、选中购物车项清理处于同一事务，失败全部回滚。商品金额及订单总金额不得超过现有 `DECIMAL(10,2)` 上限，不扩展字段精度。
12. 每个订单最多使用一张属于当前用户、当前店铺且处于有效期内的未使用优惠券；订单保存 `discount_amount` 快照。
13. 取消待处理订单时，订单状态、库存恢复和优惠券返还处于同一事务；优惠券恢复为未使用，但原到期时间不延长。返还后的同一 `user_coupon` 可再次用于新订单，因此可被多条历史订单引用，不能给 `orders.user_coupon_id` 加唯一约束；重复取消旧订单不得返还新订单正在使用的券。
14. 骑手只能领取制作中的未分配订单；领取后绑定 `rider_id`，订单仍保持制作中，随后按制作中（2）→配送中（3）→已完成（4）流转。
15. `orders.address_id` 和 `orders.user_coupon_id` 删除时按 `ON DELETE SET NULL` 清空；商品详情图随商品按 `ON DELETE CASCADE` 删除；其他外键均为 `NO ACTION`。

16. 起送价和券门槛按优惠前商品金额判断；实际优惠不超过商品金额，不抵配送费。用券核销参加下单事务，失败全部回滚。

全部迁移按数字版本顺序应用，不修改已应用的迁移；数据库一致性检查见 [演示指南](../demo.md)。

## 17. 缩写说明

| 缩写 | 含义    |
| -- | ----- |
| PK | 主键    |
| FK | 外键    |
| AI | 自动递增  |
| NN | 不允许为空 |
| UQ | 唯一约束  |
