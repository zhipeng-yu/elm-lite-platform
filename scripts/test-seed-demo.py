"""种子脚本只允许补充演示商家的目录，不给现场注册的商家塞入餐品。"""
import runpy
import subprocess
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]


class SeedDemoTest(unittest.TestCase):
    def test_leaves_other_merchants_catalog_untouched(self):
        statements = []

        def run(command, **kwargs):
            statement = kwargs['input']
            statements.append(statement)
            if '@@datadir' in statement:
                value = str(ROOT / '.local-demo/mysql')
            elif "account='demo_merchant'" in statement:
                value = '1'
            elif 'COUNT(*) FROM shop' in statement:
                value = '30'
            elif 'SELECT id, shop_name FROM shop' in statement:
                value = '1\t演示店' if 'merchant_id=1' in statement else '1\t演示店\n999\t我的手工店'
            elif 'HAVING COUNT' in statement:
                value = '0'
            else:
                value = ''
            return subprocess.CompletedProcess(command, 0, stdout=value, stderr='')

        with patch('subprocess.run', side_effect=run):
            runpy.run_path(str(ROOT / 'scripts/seed-demo.py'))
        writes = '\n'.join(s for s in statements if 'INSERT' in s)
        self.assertNotIn('sid=999', writes)
        self.assertNotIn('SELECT 999,', writes)
        self.assertNotIn('WHERE id=999', writes)


if __name__ == '__main__':
    unittest.main()
