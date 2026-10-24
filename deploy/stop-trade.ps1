$ErrorActionPreference = 'Stop'
$shopagentRoot = Split-Path $PSScriptRoot -Parent
$shopagentWork = Join-Path $shopagentRoot 'tmp\trade-linux-deploy'
$shopagentSsh = 'D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config'
& ssh.exe -F $shopagentSsh -o BatchMode=yes -T shopagent-linux 'sudo -n sh /home/shopagent/shopagent/deploy/runtime.sh stop'
$shopagentStatePath = Join-Path $shopagentWork 'windows-runtime.json'
if (Test-Path $shopagentStatePath) {
    $shopagentState = Get-Content $shopagentStatePath -Raw | ConvertFrom-Json
    if ($shopagentState.public_tunnel_pid) {
        $shopagentProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($shopagentState.public_tunnel_pid)" -ErrorAction SilentlyContinue
        if ($shopagentProcess -and $shopagentProcess.CommandLine.Contains('free@free.pinggy.io')) { Stop-Process -Id $shopagentState.public_tunnel_pid }
    }
}
foreach ($shopagentLocalPort in @(8899,8080)) {
    $shopagentListener = Get-NetTCPConnection -State Listen -LocalPort $shopagentLocalPort -ErrorAction SilentlyContinue
    if ($shopagentListener) {
        $shopagentProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($shopagentListener.OwningProcess)" -ErrorAction SilentlyContinue
        $shopagentExpected = if ($shopagentLocalPort -eq 8899) { Join-Path $PSScriptRoot 'scripts\alipay_notify_relay.py' } else { '127.0.0.1:8080:127.0.0.1:8080' }
        if ($shopagentProcess -and $shopagentProcess.CommandLine.Contains($shopagentExpected)) { Stop-Process -Id $shopagentListener.OwningProcess }
    }
}
Write-Output 'Trade site stopped. Database volume and key files retained. VMware and the separate development Redis were left unchanged.'
