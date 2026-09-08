"""真实 HTTP 冒烟：仅用于独立测试库，会创建带随机后缀的演示数据。无第三方依赖。"""
import concurrent.futures
import json
import secrets
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else 'http://127.0.0.1:8080/api/v1'
suffix = secrets.token_hex(5)
password = secrets.token_urlsafe(18)
checks = []


def call(method, path, data=None, token=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(BASE + path, data=None if data is None else json.dumps(data).encode(), headers=headers, method=method)
    try:
        response = urllib.request.urlopen(request, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    body = json.load(response)
    assert response.status == expected, f'{method} {path}: expected {expected}, got {response.status}'
    assert body['code'] == (0 if expected < 400 else expected), f'{path}: envelope mismatch'
    return body['data']


def user(name):
    call('POST', '/users', {'username': name, 'password': password, 'displayName': '演示用户'}, expected=201)
    return call('POST', '/auth/login', {'username': name, 'password': password})['accessToken']


def address(token, default=1):
    return call('POST', '/addresses', {'receiverName': '演示收货人', 'receiverPhone': '13800000000', 'addressDetail': '测试校区一号楼', 'isDefault': default}, token, 201)


account = 'merchant_' + suffix
call('POST', '/merchants', {'account': account, 'password': password, 'merchantName': '冒烟测试商家', 'contactName': '演示联系人', 'contactPhone': '13800000000'}, expected=201)
merchant = call('POST', '/merchant/auth/login', {'account': account, 'password': password})['accessToken']
assert call('GET', '/merchant/shops', token=merchant) == []
shop = call('POST', '/merchant/shops', {'shopName': '冒烟测试店铺_' + suffix, 'address': '测试校区', 'startPriceCent': 100, 'deliveryPriceCent': 100}, merchant, 201)
sid = shop['id']
category = call('POST', f'/merchant/shops/{sid}/categories', {'categoryName': '主食', 'sortOrder': 0}, merchant, 201)
product = call('POST', f'/merchant/shops/{sid}/products', {'categoryId': category['id'], 'productName': '演示餐品', 'priceCent': 1234, 'stock': 5}, merchant, 201)
pid = product['id']
checks.append('商家注册登录、空店铺、新建店铺/分类/商品')

u1, u2 = user('smoke_a_' + suffix), user('smoke_b_' + suffix)
a1, a2 = address(u1), address(u2)
call('PATCH', '/users/me', {'displayName': '修改后的昵称'}, u1)
assert call('GET', '/users/me', token=u1)['displayName'] == '修改后的昵称'
call('GET', '/merchant/shops', token=u1, expected=403)
call('GET', '/orders', token=merchant, expected=403)
call('GET', '/orders', expected=401)
checks.append('用户注册登录、个人信息、身份权限隔离')

item = call('POST', '/cart/items', {'productId': pid, 'quantity': 1}, u1, 201)
call('POST', '/orders', {'addressId': a1['id'], 'cartItemIds': [item['id']]}, u1, 409)
assert call('GET', f'/products/{pid}')['stock'] == 5
assert len(call('GET', '/cart/items', token=u1)) == 1
call('PATCH', f'/merchant/shops/{sid}', {'businessStatus': 1}, merchant)
order = call('POST', '/orders', {'addressId': a1['id'], 'cartItemIds': [item['id']]}, u1, 201)
assert order['totalAmountCent'] == 1334 and order['orderStatus'] == 0
assert call('GET', '/cart/items', token=u1) == []
assert call('GET', f'/products/{pid}')['stock'] == 4
call('GET', f"/orders/{order['id']}", token=u2, expected=403)
call('DELETE', f"/addresses/{a1['id']}", token=u1)
assert call('GET', f"/orders/{order['id']}", token=u1)['deliveryAddress'] == '测试校区一号楼'
checks.append('闭店下单失败回滚、成功下单金额/库存/清理、越权拒绝、删除地址后订单快照保留')

call('PATCH', f'/merchant/products/{pid}', {'status': 0}, merchant)
call('GET', f'/products/{pid}', expected=404)
assert call('GET', f'/merchant/shops/{sid}/products', token=merchant)[0]['status'] == 0
call('PATCH', f"/merchant/categories/{category['id']}", {'status': 0}, merchant)
assert call('GET', f'/shops/{sid}/categories') == []
assert call('GET', f'/merchant/shops/{sid}/categories', token=merchant)[0]['status'] == 0
call('PATCH', f"/merchant/categories/{category['id']}", {'status': 1}, merchant)
call('PATCH', f'/merchant/products/{pid}', {'status': 1, 'stock': 1}, merchant)
checks.append('下架商品与停用分类可在管理端找回并恢复')

a1 = address(u1)
items = [call('POST', '/cart/items', {'productId': pid, 'quantity': 1}, token, 201) for token in [u1, u2]]
def checkout(args):
    token, addr, cart = args
    try:
        return call('POST', '/orders', {'addressId': addr['id'], 'cartItemIds': [cart['id']]}, token, 201)
    except AssertionError as error:
        assert 'got 409' in str(error), str(error)
        return None
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results = list(pool.map(checkout, zip([u1, u2], [a1, a2], items)))
assert sum(result is not None for result in results) == 1
assert call('GET', f'/products/{pid}')['stock'] == 0
assert sum(len(call('GET', '/cart/items', token=token)) for token in [u1, u2]) == 1
checks.append('真实并发争抢最后一件库存：一单成功、一单409，无超卖，失败购物车保留')

with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(address, [u1, u1]))
assert sum(a['isDefault'] for a in call('GET', '/addresses', token=u1)) == 1
checks.append('并发新增默认地址后仍仅有一个默认地址')
print(json.dumps({'passed': len(checks), 'checks': checks, 'shopId': sid, 'orderId': order['id']}, ensure_ascii=False, indent=2))
