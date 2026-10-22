#!/bin/sh
set -eu

if [ -n "${SHOPAGENT_EXTRA_CA_FILE:-}" ]; then
    ca_bundle=/tmp/shopagent-ca-bundle.pem
    cat /etc/ssl/certs/ca-certificates.crt "$SHOPAGENT_EXTRA_CA_FILE" > "$ca_bundle"
    export SSL_CERT_FILE="$ca_bundle"
fi

key_file="${DEEPSEEK_API_KEY_FILE:-/run/secrets/deepseek_api_key}"
if [ ! -s "$key_file" ]; then
    printf '缺少 DeepSeek 密钥文件，无法启动模型服务。\n' >&2
    exit 1
fi

DEEPSEEK_API_KEY="$(cat "$key_file")"
export DEEPSEEK_API_KEY

exec "$@"