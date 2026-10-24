#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ -f .env.trade.local ]; then
    exec docker compose --env-file .env.trade.local -f compose.mysql.yml -f compose.runtime.yml -f compose.trade.yml "$@"
fi
exec docker compose -f compose.mysql.yml -f compose.runtime.yml -f compose.trade.yml "$@"
