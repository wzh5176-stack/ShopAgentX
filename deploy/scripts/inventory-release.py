"""Explicit additive Linux release maintenance; all credentials stay inside MySQL container."""
import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path('/home/shopagent/shopagent')
BASE = ['sudo', '-n', 'sh', str(ROOT / 'deploy/runtime.sh')]
MYSQL = BASE + ['exec', '-T', 'mysql', 'sh', '-c',
    'export MYSQL_PWD="$(cat /run/secrets/mysql_root_password)"; '
    'exec mysql --protocol=socket --user=root --database=shopagent --batch --skip-column-names --default-character-set=utf8mb4 --binary-as-hex']

def sql(statement):
    result = subprocess.run(MYSQL, input=statement, text=True, capture_output=True, check=True)
    return result.stdout.strip()

def private_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    os.chmod(path, 0o600)

def snapshot(limits=None):
    result = {}
    tables = list(limits) if limits is not None else sql('SHOW TABLES;').splitlines()
    for table in tables:
        if not re.fullmatch('[a-z_]+', table): raise RuntimeError('Unexpected table name')
        key = sql("SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='shopagent' AND TABLE_NAME='"
                  + table + "' AND CONSTRAINT_NAME='PRIMARY' ORDER BY ORDINAL_POSITION;")
        if not re.fullmatch('[a-z_]+', key): raise RuntimeError('Expected one primary key')
        # Only catalog tables gain rows during this release. Other PKs may be UUID strings.
        maximum = (limits[table]['max_id'] if limits else
                   int(sql(f'SELECT COALESCE(MAX(`{key}`),0) FROM `{table}`;')) if table in {'products', 'product_inventory'} else None)
        where = '' if maximum is None else f' WHERE `{key}`<={int(maximum)}'
        data = sql(f'SELECT * FROM `{table}`{where} ORDER BY `{key}`;')
        count = int(sql(f'SELECT COUNT(*) FROM `{table}`{where};'))
        result[table] = {'rows':count, 'max_id':maximum, 'sha256':hashlib.sha256(data.encode()).hexdigest()}
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['backup', 'migrate', 'verify'])
    parser.add_argument('--release', type=Path, required=True)
    args = parser.parse_args()
    state = args.release / 'private-before.json'
    if args.command == 'backup':
        if state.exists(): raise RuntimeError('Existing release backup state must not be overwritten')
        backups = ROOT / 'backups'; backups.mkdir(exist_ok=True); os.chmod(backups, 0o700)
        backup = backups / ('before-inventory-' + datetime.now().strftime('%Y%m%d-%H%M%S') + '.sql')
        with backup.open('xb') as out:
            os.chmod(backup, 0o600)
            subprocess.run(BASE + ['exec', '-T', 'mysql', 'sh', '-c',
                'export MYSQL_PWD="$(cat /run/secrets/mysql_root_password)"; '
                'exec mysqldump --user=root --single-transaction --quick --no-tablespaces --set-gtid-purged=OFF '
                '--default-character-set=utf8mb4 --hex-blob --routines --events --triggers shopagent'],
                stdout=out, stderr=subprocess.PIPE, check=True)
        if backup.stat().st_size < 1000: raise RuntimeError('Unexpectedly small backup')
        report = {'backup':str(backup),'bytes':backup.stat().st_size,
                  'sha256':hashlib.sha256(backup.read_bytes()).hexdigest(), 'tables':snapshot()}
        private_json(state, report)
        print(json.dumps({k:v for k,v in report.items() if k!='tables'}))
    elif args.command == 'migrate':
        before = json.loads(state.read_text())
        if snapshot(before['tables']) != before['tables']: raise RuntimeError('Old data changed since backup')
        exists = sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='shopagent' AND table_name='inventory_replenishments';")
        if exists == '0': sql((args.release / 'backend/sql/028_create_inventory_replenishments.sql').read_text())
        # Deployment account differs from the native Windows @127.0.0.1 account.
        sql("GRANT SELECT,INSERT ON shopagent.inventory_replenishments TO 'shopagent_app'@'%';")
        if snapshot(before['tables']) != before['tables']: raise RuntimeError('Migration changed existing rows')
        print(json.dumps({'schema_added':exists=='0','old_rows_preserved':True,'tables':sql('SHOW TABLES;').splitlines()}))
    else:
        before = json.loads(state.read_text())
        preserved = snapshot(before['tables']) == before['tables']
        report = {'original_rows_preserved':preserved,'visible_products':int(sql('SELECT COUNT(*) FROM products WHERE status=1 AND deleted=0;')),
                  'tables':sql('SHOW TABLES;').splitlines()}
        private_json(args.release/'preservation.json', report)
        print(json.dumps(report))
        if not preserved: raise RuntimeError('Original rows changed; inspect before any further changes')

if __name__ == '__main__': main()
