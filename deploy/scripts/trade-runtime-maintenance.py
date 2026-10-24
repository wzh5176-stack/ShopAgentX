"""Explicit maintenance commands for the local VMware deployment; never prints secrets."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
from datetime import datetime

ROOT = Path('/home/shopagent/shopagent')
RUNTIME = ROOT / 'deploy/runtime.sh'
BASE = ['sudo', '-n', 'sh', str(RUNTIME)]
ORIGINAL = ['app_users', 'cart_actions', 'cart_items', 'chat_conversations', 'chat_messages', 'products']
NEW = ['user_addresses', 'product_inventory', 'shop_orders', 'order_items', 'payment_attempts', 'mock_payment_receipts']
TAG = 'trade-20261024'
REPORTS = ROOT / 'deploy/verification'

def run(args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)

def sql(query):
    cmd = BASE + ['exec', '-T', 'mysql', 'sh', '-c',
        'export MYSQL_PWD="$(cat /run/secrets/mysql_root_password)"; '
        'exec mysql --protocol=socket --user=root --database=shopagent --batch '
        '--default-character-set=utf8mb4 --binary-as-hex']
    return run(cmd, input=query, text=True, capture_output=True).stdout

def table_names():
    return sql('SHOW TABLES;').strip().splitlines()[1:]

def snapshot(executor=None, limits=None):
    query = executor or sql
    result = {}
    for name in ORIGINAL:
        keys = query(f"SHOW KEYS FROM `{name}` WHERE Key_name='PRIMARY';").strip().splitlines()
        index = keys[0].split('\t').index('Column_name')
        columns = [line.split('\t')[index] for line in keys[1:]]
        if not columns or any(not re.fullmatch(r'[a-z_]+', c) for c in columns):
            raise RuntimeError('Unexpected primary key')
        ordering = ','.join('`' + c + '`' for c in columns)
        suffix = '' if limits is None else ' LIMIT ' + str(int(limits[name]['rows']))
        raw = query(f'SELECT * FROM `{name}` ORDER BY {ordering}{suffix};')
        count = int(query(f'SELECT COUNT(*) FROM `{name}`;').strip().splitlines()[-1])
        if limits is not None:
            count = min(count, limits[name]['rows'])
        result[name] = {'rows': count, 'sha256': hashlib.sha256(raw.encode()).hexdigest()}
    return result

def save(name, report):
    REPORTS.mkdir(parents=True, exist_ok=True)
    p = REPORTS / name
    p.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    os.chmod(p, 0o600)

def inspect_backup():
    backups = ROOT / 'backups'
    backups.mkdir(exist_ok=True)
    os.chmod(backups, 0o700)
    final = backups / ('shopagent-before-trade-' + datetime.now().strftime('%Y%m%d-%H%M%S') + '.sql')
    partial = Path(str(final) + '.partial')
    with partial.open('xb') as out:
        os.chmod(partial, 0o600)
        run(BASE + ['exec', '-T', 'mysql', 'sh', '-c',
            'export MYSQL_PWD="$(cat /run/secrets/mysql_root_password)"; '
            'exec mysqldump --protocol=socket --user=root --single-transaction --quick '
            '--no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 '
            '--hex-blob --routines --events --triggers shopagent'], stdout=out, stderr=subprocess.PIPE)
    if partial.stat().st_size < 1000:
        raise RuntimeError('Unexpectedly small backup; partial retained')
    partial.rename(final)
    report = {'created_at': datetime.now().isoformat(),
        'version': sql('SELECT VERSION();').strip().splitlines()[-1],
        'backup': {'path': str(final), 'bytes': final.stat().st_size,
                   'sha256': hashlib.file_digest(final.open('rb'), 'sha256').hexdigest()},
        'tables': table_names(), 'original_data': snapshot(),
        'grants': sql("SHOW GRANTS FOR 'shopagent_app'@'%';").splitlines()[1:],
        'columns': sql('SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS '
                       "WHERE TABLE_SCHEMA='shopagent' ORDER BY TABLE_NAME,ORDINAL_POSITION;")}
    save('trade-before-20261024.json', report)
    print(json.dumps(report, ensure_ascii=False))

def final_check():
    before = json.loads((REPORTS / 'trade-before-20261024.json').read_text())
    old_rows = snapshot(limits=before['original_data'])
    report = {'created_at':datetime.now().isoformat(),
        'original_rows_unchanged':old_rows == before['original_data'],
        'original_rows':old_rows,
        'counts':{name:int(sql(f'SELECT COUNT(*) FROM `{name}`;').strip().splitlines()[-1]) for name in ORIGINAL+NEW},
        'inventory':sql('SELECT product_id,available_quantity,reserved_quantity FROM product_inventory ORDER BY product_id;'),
        'pending_orders':int(sql("SELECT COUNT(*) FROM shop_orders WHERE status='PENDING_PAYMENT';").strip().splitlines()[-1]),
        'pending_payments':int(sql("SELECT COUNT(*) FROM payment_attempts WHERE status='PENDING';").strip().splitlines()[-1]),
        'reserved_quantity':int(sql('SELECT COALESCE(SUM(reserved_quantity),0) FROM product_inventory;').strip().splitlines()[-1])}
    save('trade-final-data-20261024.json',report)
    print(json.dumps(report))

def restore_check():
    before = json.loads((REPORTS / 'trade-before-20261024.json').read_text())
    name = 'shopagent-trade-restore-20261024'
    if subprocess.run(['sudo', '-n', 'docker', 'inspect', name], capture_output=True).returncode == 0:
        raise RuntimeError('Dedicated restore container already exists; inspect before reuse')
    run(['sudo','-n','docker','run','-d','--name',name,'--network','none',
         '--memory','768m','--env','MYSQL_ALLOW_EMPTY_PASSWORD=yes','--env','MYSQL_DATABASE=shopagent',
         '--mount','type=volume,src='+name+',dst=/var/lib/mysql',
         'mysql:8.4.11','--skip-networking'], capture_output=True)
    cmd = ['sudo','-n','docker','exec','-i',name,'mysql','--protocol=socket','--user=root',
           '--database=shopagent','--batch','--default-character-set=utf8mb4','--binary-as-hex']
    try:
        deadline = time.monotonic() + 90
        while True:
            ready = subprocess.run(cmd, input='SELECT 1;', text=True, capture_output=True)
            if ready.returncode == 0:
                break
            if time.monotonic() >= deadline:
                raise RuntimeError('Restore database did not become ready')
            time.sleep(1)
        with Path(before['backup']['path']).open('rb') as data:
            run(cmd, stdin=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        def restored_sql(q):
            return run(cmd, input=q, text=True, capture_output=True).stdout
        restored = snapshot(restored_sql)
        report = {'backup': before['backup'], 'isolated_network': 'none',
                  'restored_original_data': restored,
                  'all_six_tables_match': restored == before['original_data']}
        save('trade-restore-20261024.json', report)
        if not report['all_six_tables_match']:
            raise RuntimeError('Restore mismatch')
        print(json.dumps(report))
    finally:
        run(['sudo','-n','docker','stop',name], capture_output=True)

def migrate(schema_file):
    before = json.loads((REPORTS / 'trade-before-20261024.json').read_text())
    if snapshot() != before['original_data']:
        raise RuntimeError('Original tables changed since backup; migration stopped')
    schema = json.loads(Path(schema_file).read_text())
    existing = set(table_names())
    if set(schema) != set(NEW):
        raise RuntimeError('Unexpected schema export')
    # Refuse partially upgraded deployments; review existing trade tables explicitly first.
    if existing.intersection(NEW):
        raise RuntimeError('Trade tables already exist; inspect before rerunning')
    applied = []
    for name in NEW:
        statement = schema[name]
        if not statement.startswith('CREATE TABLE `' + name + '`'):
            raise RuntimeError('DDL identity mismatch')
        sql(statement + ';')
        applied.append(name)
    sql('INSERT INTO product_inventory(product_id,available_quantity,reserved_quantity) '
        'SELECT p.id,CASE WHEN p.status=1 AND p.deleted=0 THEN 20 ELSE 0 END,0 '
        'FROM products p WHERE NOT EXISTS (SELECT 1 FROM product_inventory i WHERE i.product_id=p.id);')
    privileges = {
        'user_addresses': 'SELECT,INSERT,UPDATE,DELETE',
        'product_inventory': 'SELECT,INSERT,UPDATE',
        'shop_orders': 'SELECT,INSERT,UPDATE',
        'order_items': 'SELECT,INSERT',
        'payment_attempts': 'SELECT,INSERT,UPDATE',
        'mock_payment_receipts': 'SELECT,INSERT',
    }
    for name, rights in privileges.items():
        sql(f"GRANT {rights} ON shopagent.`{name}` TO 'shopagent_app'@'%';")
    after = snapshot()
    report = {'created_at': datetime.now().isoformat(), 'created_tables': applied,
        'tables': table_names(), 'original_data_preserved': after == before['original_data'],
        'original_data': after,
        'grants': sql("SHOW GRANTS FOR 'shopagent_app'@'%';").splitlines()[1:],
        'inventory': sql('SELECT * FROM product_inventory ORDER BY product_id;')}
    save('trade-migration-20261024.json', report)
    if not report['original_data_preserved']:
        raise RuntimeError('Original data changed; inspect before starting services')
    print(json.dumps(report, ensure_ascii=False))

if __name__ == '__main__':
    try:
        if sys.argv[1] == 'backup':
            inspect_backup()
        elif sys.argv[1] == 'restore':
            restore_check()
        elif sys.argv[1] == 'final':
            final_check()
        elif sys.argv[1] == 'migrate':
            migrate(sys.argv[2])
        else:
            raise ValueError('Unknown explicit command')
    except subprocess.CalledProcessError as e:
        # No command lines, raw response bodies or secret-bearing configuration dumps.
        diagnostic = e.stderr or ''
        if isinstance(diagnostic, bytes):
            diagnostic = diagnostic.decode(errors='replace')
        diagnostic = re.sub(r'(?i)(password|token|api[_-]?key)\s*[=:]\s*\S+', r'\1=[redacted]', diagnostic)
        print(json.dumps({'failed': True, 'exit_code': e.returncode,
                          'phase': sys.argv[1], 'diagnostic': diagnostic[:1500]}, ensure_ascii=False))
        sys.exit(1)
