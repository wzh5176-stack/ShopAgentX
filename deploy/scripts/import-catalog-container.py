"""Operator-only import. Consume existing DB secret without placing it in arguments or output."""
import os
from pathlib import Path
import runpy

os.environ['SHOPAGENT_DB_PASSWORD'] = Path('/run/secrets/db_password').read_text().strip()
runpy.run_path('scripts/import_demo_catalog.py', run_name='__main__')
