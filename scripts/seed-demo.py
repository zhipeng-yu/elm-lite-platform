"""补充独立本机演示库至至少30家店，每家补充6款餐品；不重置已有库存。"""
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((ROOT / 'database/init/demo-catalog.json').read_text(encoding='utf-8'))
MYSQL = ['mysql', '--no-defaults', '--protocol=TCP', '--host=127.0.0.1',
         '--port=13317', '--user=root', '--default-character-set=utf8mb4',
         '--batch', '--skip-column-names', 'elm_lite']


def sql(statement):
    result = subprocess.run(MYSQL, input=statement, encoding='utf-8', capture_output=True, check=True)
    return result.stdout.strip()


def literal(value):
    return "_utf8mb4 0x" + value.encode('utf-8').hex()


# 只连接启动脚本创建的独立库，避免把演示数据导入其他本地项目。
assert Path(sql('SELECT @@datadir')).resolve() == (ROOT / 'backend/target/local-demo/mysql').resolve(), 'Not the isolated demo database'
merchant_id = int(sql("SELECT id FROM merchant WHERE account='demo_merchant'"))
before = int(sql('SELECT COUNT(*) FROM shop'))
existing_names = set(sql('SELECT shop_name FROM shop').splitlines())
statements = ['START TRANSACTION;']
added = 0
for index in range(5):
    for kind, group in CATALOG.items():
        name = group['shops'][index]
        if before + added >= 30 or name in existing_names:
            continue
        delivery = [0, 2, 3, 1, 2][index]
        minimum = 10 if kind == 'tea' else [15, 20, 10, 15, 12][index]
        image = f'/images/food/{kind}.jpg'
        address = f'校园生活区{index + 1}号楼餐饮街'
        statements.append(f'''INSERT INTO shop (merchant_id, shop_name, description, address, image_url, start_price, delivery_price, business_status)
            VALUES ({merchant_id}, {literal(name)}, {literal(group['description'])}, {literal(address)}, {literal(image)}, {minimum}, {delivery}, 1);''')
        added += 1
statements.append('COMMIT;')
sql('\n'.join(statements))

shops = [line.split('\t') for line in sql('SELECT id, shop_name FROM shop ORDER BY id').splitlines()]
statements = ['START TRANSACTION;']
for sid, name in shops:
    kind = next((key for key, group in CATALOG.items() if name in group['shops']), 'rice')
    group = CATALOG[kind]
    image = literal(f'/images/food/{kind}.jpg')
    category = literal('店内餐品')
    statements.append(f'''UPDATE shop SET image_url={image} WHERE id={sid} AND (image_url IS NULL OR image_url='');
        INSERT IGNORE INTO product_category (shop_id, category_name, sort_order, status) VALUES ({sid}, {category}, 10, 1);''')
    for product_name, price_cent in group['products']:
        product = literal(product_name)
        statements.append(f'''INSERT INTO product (shop_id, category_id, product_name, description, image_url, price, stock, status)
            SELECT {sid}, c.id, {product}, {literal('单人份，现点现做')}, {image}, {price_cent}/100, 100, 1
            FROM product_category c WHERE c.shop_id={sid} AND c.category_name={category}
            AND NOT EXISTS (SELECT 1 FROM product p WHERE p.shop_id={sid} AND p.product_name={product});''')
statements.append('COMMIT;')
sql('\n'.join(statements))

# 可重复执行的最小校验；不要求卖完或手工下架的商品自动恢复。
count = int(sql('SELECT COUNT(*) FROM shop'))
too_small = int(sql('SELECT COUNT(*) FROM (SELECT s.id FROM shop s LEFT JOIN product p ON p.shop_id=s.id GROUP BY s.id HAVING COUNT(p.id)<6) x'))
assert count >= 30 and too_small == 0, 'Catalog import incomplete'
print(f'Demo catalog ready: {count} shops, at least 6 products each; added {added} shops.')
