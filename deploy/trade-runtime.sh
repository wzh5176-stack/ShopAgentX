#!/bin/sh
set -eu
cd "$(dirname "$0")"
compose_files="-f compose.mysql.yml -f compose.runtime.yml -f compose.trade.yml"
if [ -f .inventory-runtime-enabled ]; then
    compose_files="$compose_files -f compose.inventory.yml"
fi
if [ -f .env.trade.local ]; then
    exec docker compose --env-file .env.trade.local $compose_files "$@"
fi
exec docker compose $compose_files "$@"
