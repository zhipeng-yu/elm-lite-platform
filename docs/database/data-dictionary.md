# 数据库字段说明

## 1. 基本信息

* 数据库：MySQL 5.5
* 字符集：utf8mb4
* 存储引擎：InnoDB
* 主键：BIGINT 自增
* 金额：DECIMAL(10,2)
* 状态：TINYINT
* 创建时间：TIMESTAMP
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
| sort_order    | INT UNSIGNED | NN、默认0   | 显示顺序           |
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

## 7. cart_item 购物车明细表

| 字段         | 类型           | 约束       | 说明      |
| ---------- | ------------ | -------- | ------- |
| id         | BIGINT       | PK、AI、NN | 购物车明细编号 |
| user_id    | BIGINT       | FK、NN    | 所属用户编号  |
| product_id | BIGINT       | FK、NN    | 商品编号    |
| quantity   | INT UNSIGNED | NN、默认1   | 商品数量    |
| created_at | TIMESTAMP    | NN、自动生成  | 创建时间    |
| updated_at | DATETIME     | 可空       | 最后修改时间  |

同一用户与同一商品只能存在一条购物车记录。

## 8. delivery_address 收货地址表

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

同一用户只能设置一个默认收货地址，此规则由 Service 层保证。

## 9. orders 订单表

| 字段               | 类型                     | 约束        | 说明         |
| ---------------- | ---------------------- | --------- | ---------- |
| id               | BIGINT                 | PK、AI、NN  | 订单编号       |
| order_no         | VARCHAR(32)            | UQ、NN     | 对外展示的订单号   |
| user_id          | BIGINT                 | FK、NN     | 下单用户编号     |
| shop_id          | BIGINT                 | FK、NN     | 所属店铺编号     |
| address_id       | BIGINT                 | FK、可空     | 下单时选择的地址编号 |
| receiver_name    | VARCHAR(50)            | NN        | 收货人姓名快照    |
| receiver_phone   | VARCHAR(20)            | NN        | 收货人电话快照    |
| delivery_address | VARCHAR(255)           | NN        | 收货地址快照     |
| product_amount   | DECIMAL(10,2) UNSIGNED | NN、默认0.00 | 商品总金额      |
| delivery_fee     | DECIMAL(10,2) UNSIGNED | NN、默认0.00 | 配送费        |
| total_amount     | DECIMAL(10,2) UNSIGNED | NN、默认0.00 | 订单总金额      |
| order_status     | TINYINT                | NN、默认0    | 订单状态       |
| remark           | VARCHAR(255)           | 可空        | 订单备注       |
| created_at       | TIMESTAMP              | NN、自动生成   | 创建时间       |
| updated_at       | DATETIME               | 可空        | 最后修改时间     |

订单状态：

| 状态值 | 含义  |
| --- | --- |
| 0   | 待处理 |
| 1   | 已确认 |
| 2   | 制作中 |
| 3   | 配送中 |
| 4   | 已完成 |
| 5   | 已取消 |

## 10. order_item 订单明细表

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

## 11. 业务约束

1. 商品价格、库存和购物车数量不能为负数。
2. 商品所属分类必须属于商品对应的店铺。
3. 一个订单只能包含同一家店铺的商品。
4. 订单必须至少包含一条订单明细。
5. 商品小计等于商品单价乘以购买数量。
6. 订单总金额等于商品总金额加配送费。
7. 下单数量不能超过商品库存。
8. 订单保存商品和地址快照，避免源数据修改后影响历史订单。

## 12. 缩写说明

| 缩写 | 含义    |
| -- | ----- |
| PK | 主键    |
| FK | 外键    |
| AI | 自动递增  |
| NN | 不允许为空 |
| UQ | 唯一约束  |
