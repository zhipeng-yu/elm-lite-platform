"""真实 MySQL/HTTP 回归，仅允许本项目 .local-demo 测试库；只生成虚构数据。"""
import concurrent.futures
from datetime import datetime, timedelta
import json
import os
from pathlib import Path
import runpy
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:5180/api/v1'
if len(sys.argv) > 1 and sys.argv[1] != BASE:
    raise SystemExit('Only the isolated local demo proxy is supported')


def sql(statement):
    result = subprocess.run(['mysql', '--no-defaults', '--host=127.0.0.1', '--port=13317',
                             '--user=root', '--batch', '--skip-column-names', 'elm_lite'],
                            input=statement, encoding='utf-8', capture_output=True, check=True)
    return result.stdout.strip()


assert Path(sql('SELECT @@datadir')).resolve() == (ROOT / '.local-demo/mysql').resolve()
expected_tables = {'users', 'merchant', 'shop', 'product_category', 'product', 'cart_item',
                   'delivery_address', 'orders', 'order_item', 'product_detail_image',
                   'admin_account', 'coupon', 'user_coupon', 'rider'}
assert set(sql('SHOW TABLES').splitlines()) == expected_tables
assert sql('SELECT COUNT(*) FROM orders WHERE total_amount <> product_amount - discount_amount + delivery_fee') == '0'
assert sql('SELECT COUNT(*) FROM product p JOIN product_category c ON c.id=p.category_id WHERE p.shop_id<>c.shop_id') == '0'
foreign_keys = sql("SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='elm_lite' AND REFERENCED_TABLE_NAME IS NOT NULL").splitlines()
assert len(foreign_keys) >= 17
for key in foreign_keys:
    table, column, parent, parent_column = key.split('\t')
    assert all(value.replace('_', '').isalnum() for value in [table, column, parent, parent_column])
    assert sql(f'SELECT COUNT(*) FROM `{table}` c LEFT JOIN `{parent}` p ON c.`{column}`=p.`{parent_column}` WHERE c.`{column}` IS NOT NULL AND p.`{parent_column}` IS NULL') == '0'

# 复用第一阶段回归，覆盖库存竞争、默认地址、权限和快照。
sys.argv = [sys.argv[0], BASE]
base = runpy.run_path(str(ROOT / 'scripts/stage1-smoke.py'))
call = base['call']
merchant, user = base['merchant'], base['u1']
sid, pid, address = base['sid'], base['pid'], base['a1']
suffix, password = base['suffix'], base['password']
checks = ['V1—V7 表、外键、引用、金额和分类归属核对']
for item in call('GET', '/cart/items', token=user):
    call('DELETE', f"/cart/items/{item['id']}", token=user)
call('PATCH', f'/merchant/products/{pid}', {'priceCent': 3000, 'stock': 10,
     'detailImageUrls': ['/images/food/rice.jpg', '/images/food/noodles.jpg', '/images/food/tea.jpg']}, merchant)
assert len(call('GET', f'/products/{pid}')['detailImageUrls']) == 3
now = datetime.now()
coupon = call('POST', f'/merchant/shops/{sid}/coupons', {'name': '验收满20减5',
    'thresholdCent': 2000, 'discountCent': 500, 'startsAt': (now-timedelta(days=1)).isoformat(timespec='seconds'),
    'expiresAt': (now+timedelta(days=1)).isoformat(timespec='seconds')}, merchant, 201)
call('POST', f"/coupons/{coupon['id']}/claims", token=user, expected=201)
call('POST', f"/coupons/{coupon['id']}/claims", token=user, expected=409)
uc = next(c for c in call('GET', '/coupons/mine', token=user) if c['couponId'] == coupon['id'])


def order():
    item = call('POST', '/cart/items', {'productId': pid, 'quantity': 2}, user, 201)
    return call('POST', '/orders', {'addressId': address['id'], 'cartItemIds': [item['id']],
                'userCouponId': uc['userCouponId'], 'remark': '验收备注：不要香菜'}, user, 201)


first = order()
assert first['totalAmountCent'] == 5600 and first['discountAmountCent'] == 500
call('POST', f"/orders/{first['id']}/cancel", token=user)
assert call('GET', f'/products/{pid}')['stock'] == 10
second = order()
call('POST', f"/orders/{first['id']}/cancel", token=user)
assert call('GET', f'/products/{pid}')['stock'] == 8
assert next(c for c in call('GET', '/coupons/mine', token=user) if c['userCouponId'] == uc['userCouponId'])['status'] == 'USED'
checks.append('商品三图、领券、用券、取消返券、重用券后重复取消旧订单')
oid = second['id']
call('POST', f'/merchant/orders/{oid}/confirm', token=merchant)
call('POST', f'/merchant/orders/{oid}/prepare', token=merchant)
call('POST', f'/orders/{oid}/cancel', token=user, expected=409)
riders = []
for index in range(2):
    name = f'qa_rider_{suffix}_{index}'
    call('POST', '/riders', {'username': name, 'password': password, 'displayName': '验收骑手'}, expected=201)
    riders.append(call('POST', '/rider/auth/login', {'username': name, 'password': password})['accessToken'])


def claim(token):
    try:
        call('POST', f'/rider/orders/{oid}/claim', token=token)
        return True
    except AssertionError as exc:
        assert 'got 409' in str(exc)
        return False


with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    winners = list(pool.map(claim, riders))
assert sum(winners) == 1
rider = riders[winners.index(True)]
assert any(task['id'] == oid for task in call('GET', '/rider/orders', token=rider))
call('GET', f'/rider/orders/{oid}', token=riders[winners.index(False)], expected=403)
assert call('GET', f'/rider/orders/{oid}', token=rider)['remark'] == '验收备注：不要香菜'
assert sql(f'SELECT COUNT(*) FROM orders WHERE id={oid} AND rider_id IS NOT NULL AND order_status=2') == '1'
call('POST', f'/rider/orders/{oid}/dispatch', token=rider)
call('POST', f'/rider/orders/{oid}/complete', token=rider)
call('POST', f'/rider/orders/{oid}/complete', token=rider)
assert call('GET', f'/orders/{oid}', token=user)['orderStatus'] == 4
checks.append('商家履约、骑手抢单、任务归属、配送详情和重复送达')

if os.environ.get('ADMIN_USERNAME') and os.environ.get('ADMIN_PASSWORD'):
    admin = call('POST', '/admin/auth/login', {'username': os.environ['ADMIN_USERNAME'],
                  'password': os.environ['ADMIN_PASSWORD']})['accessToken']
    assert call('GET', f'/admin/orders/{oid}', token=admin)['discountAmountCent'] == 500
    mid = next(m['id'] for m in call('GET', '/admin/merchants', token=admin) if m['account'] == base['account'])
    uid = call('GET', '/users/me', token=user)['id']
    call('PATCH', f'/admin/users/{uid}', {'status': 0}, admin)
    call('GET', '/orders', token=user, expected=403)
    call('PATCH', f'/admin/users/{uid}', {'status': 1}, admin)
    call('PATCH', f'/admin/merchants/{mid}', {'status': 0}, admin)
    call('GET', '/merchant/shops', token=merchant, expected=403)
    call('PATCH', f'/admin/merchants/{mid}', {'status': 1}, admin)
    checks.append('管理员金额快照与账号启停后的旧会话权限')
else:
    checks.append('管理员检查未运行：未提供初始化环境变量')

# 重新补种子后，人工创建的这家店仍只有一件商品。
subprocess.run([sys.executable, str(ROOT / 'scripts/seed-demo.py')], check=True)
assert len(call('GET', f'/merchant/shops/{sid}/products', token=merchant)) == 1
assert call('GET', f'/products/{pid}')['stock'] == 8
checks.append('重复补演示数据不污染手工店铺、不重置库存')
# 留下一单制作中的虚构任务，供人工从骑手页面继续验收。
item = call('POST', '/cart/items', {'productId': pid, 'quantity': 1}, user, 201)
pending = call('POST', '/orders', {'addressId': address['id'], 'cartItemIds': [item['id']],
               'remark': '虚构配送验收，请在手机端确认地址、电话与备注'}, user, 201)
call('POST', f"/merchant/orders/{pending['id']}/confirm", token=merchant)
call('POST', f"/merchant/orders/{pending['id']}/prepare", token=merchant)
assert call('GET', f'/products/{pid}')['stock'] == 7
print(json.dumps({'checks': checks, 'shopId': sid, 'productId': pid, 'orderId': oid,
                  'pendingRiderOrderId': pending['id']}, ensure_ascii=False, indent=2))
