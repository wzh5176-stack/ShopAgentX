#!/bin/sh
set -eu
cd "$(dirname "$0")"
exec docker compose -f compose.mysql.yml -f compose.runtime.yml "$@"
