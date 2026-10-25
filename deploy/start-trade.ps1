param([switch]$PublicNotify)
$ErrorActionPreference = 'Stop'
$shopagentRoot = Split-Path $PSScriptRoot -Parent
$shopagentWork = Join-Path $shopagentRoot 'tmp\trade-linux-deploy'
$shopagentSsh = 'D:\VMware work\ShopAgent-Ubuntu24\access\ssh_config'
New-Item -ItemType Directory -Path $shopagentWork -Force | Out-Null

# Publishable templates stay separate from this machine's ignored runtime URL.
& scp.exe -q -F $shopagentSsh (Join-Path $PSScriptRoot 'runtime.sh') (Join-Path $PSScriptRoot 'compose.trade.yml') (Join-Path $PSScriptRoot 'compose.inventory.yml') 'shopagent-linux:/home/shopagent/shopagent/deploy/'
if ($LASTEXITCODE -ne 0) { throw 'Runtime template upload failed' }
$shopagentLocalEnv = Join-Path $PSScriptRoot '.env.trade.local'
if (Test-Path $shopagentLocalEnv) {
    & scp.exe -q -F $shopagentSsh $shopagentLocalEnv 'shopagent-linux:/home/shopagent/shopagent/deploy/.env.trade.local'
    if ($LASTEXITCODE -ne 0) { throw 'Local runtime settings upload failed' }
}

# The existing VMware guest must already be running. This script changes no virtualization settings.
& ssh.exe -F $shopagentSsh -o BatchMode=yes -T shopagent-linux 'sudo -n sh /home/shopagent/shopagent/deploy/runtime.sh up -d --wait --wait-timeout 180'
if ($LASTEXITCODE -ne 0) { throw 'Linux startup failed. Confirm the existing ShopAgent VMware guest is running.' }

$shopagentPort = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
if (-not $shopagentPort) {
    $shopagentForward = Start-Process ssh.exe -ArgumentList @('-F',('"'+$shopagentSsh+'"'),'-o','BatchMode=yes','-o','ExitOnForwardFailure=yes','-o','ServerAliveInterval=30','-o','ServerAliveCountMax=3','-N','-L','127.0.0.1:8080:127.0.0.1:8080','shopagent-linux') -WindowStyle Hidden -RedirectStandardError (Join-Path $shopagentWork 'forward.err.log') -PassThru
} else {
    $shopagentCommand = (Get-CimInstance Win32_Process -Filter "ProcessId=$($shopagentPort.OwningProcess)").CommandLine
    if (-not $shopagentCommand.Contains('127.0.0.1:8080:127.0.0.1:8080')) { throw 'Port 8080 belongs to another program; it was left unchanged.' }
}

if ($PublicNotify) {
    $shopagentRelayPath = Join-Path $PSScriptRoot 'scripts\alipay_notify_relay.py'
    $shopagentRelayPort = Get-NetTCPConnection -State Listen -LocalPort 8899 -ErrorAction SilentlyContinue
    if ($shopagentRelayPort) {
        $shopagentCommand = (Get-CimInstance Win32_Process -Filter "ProcessId=$($shopagentRelayPort.OwningProcess)").CommandLine
        if (-not $shopagentCommand.Contains($shopagentRelayPath) -or -not $shopagentCommand.Contains('--upstream-port 8080')) { throw 'Port 8899 belongs to another configuration; it was left unchanged.' }
    } else {
        $shopagentRelay = Start-Process (Join-Path $shopagentRoot 'ai-service\.venv\Scripts\python.exe') -ArgumentList @($shopagentRelayPath,'--upstream-port','8080') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $shopagentWork 'relay.out.log') -RedirectStandardError (Join-Path $shopagentWork 'relay.err.log') -PassThru
    }
    $shopagentStatePath = Join-Path $shopagentWork 'windows-runtime.json'
    $shopagentState = if (Test-Path $shopagentStatePath) { Get-Content $shopagentStatePath -Raw | ConvertFrom-Json } else { $null }
    $shopagentTunnelAlive = $false
    if ($shopagentState -and $shopagentState.public_tunnel_pid) {
        $shopagentProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($shopagentState.public_tunnel_pid)" -ErrorAction SilentlyContinue
        $shopagentTunnelAlive = $shopagentProcess -and $shopagentProcess.CommandLine.Contains('free@free.pinggy.io')
    }
    if (-not $shopagentTunnelAlive) {
        $shopagentTunnelKey = Join-Path $shopagentRoot 'secrets\pinggy-tunnel-ed25519'
        if (-not (Test-Path $shopagentTunnelKey)) {
            & (Join-Path $shopagentRoot 'ai-service\.venv\Scripts\python.exe') -c "import subprocess,sys; subprocess.run(['ssh-keygen.exe','-q','-t','ed25519','-N','','-f',sys.argv[1]],check=True,capture_output=True)" $shopagentTunnelKey
            if ($LASTEXITCODE -ne 0) { throw 'Dedicated tunnel key creation failed' }
        }
        $shopagentTunnel = Start-Process ssh.exe -ArgumentList @('-F','NUL','-T','-p','443','-i',('"'+$shopagentTunnelKey+'"'),'-o','BatchMode=yes','-o','StrictHostKeyChecking=accept-new','-o',('UserKnownHostsFile='+$shopagentWork+'\pinggy-known-hosts'),'-o','IdentityAgent=none','-o','IdentitiesOnly=yes','-o','PreferredAuthentications=publickey','-o','PasswordAuthentication=no','-o','ServerAliveInterval=30','-o','ServerAliveCountMax=3','-o','ExitOnForwardFailure=yes','-R','0:127.0.0.1:8899','free@free.pinggy.io') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $shopagentWork 'pinggy.out.log') -RedirectStandardError (Join-Path $shopagentWork 'pinggy.err.log') -PassThru
        $shopagentTunnelId = $shopagentTunnel.Id
    } else { $shopagentTunnelId = $shopagentState.public_tunnel_pid }
    $shopagentOrigin = $null
    for ($shopagentAttempt=0; $shopagentAttempt -lt 60; $shopagentAttempt++) {
        $shopagentLog = Get-Content (Join-Path $shopagentWork 'pinggy.out.log') -Raw -ErrorAction SilentlyContinue
        if ($shopagentLog -match 'https://[a-zA-Z0-9.-]+\.(?:pinggy-free\.link|pinggy\.link|free\.pinggy\.net)') { $shopagentOrigin=$Matches[0]; break }
        Start-Sleep -Seconds 1
    }
    if (-not $shopagentOrigin) { throw 'Public tunnel did not return a domain. Local website remains available.' }
    $shopagentNewEnv = 'SHOPAGENT_ALIPAY_SANDBOX_NOTIFY_URL=' + $shopagentOrigin + '/api/payments/alipay/notify' + "`n"
    $shopagentOldEnv = if (Test-Path $shopagentLocalEnv) { [IO.File]::ReadAllText($shopagentLocalEnv) } else { '' }
    if ($shopagentNewEnv -ne $shopagentOldEnv) {
        [IO.File]::WriteAllText($shopagentLocalEnv,$shopagentNewEnv,[Text.UTF8Encoding]::new($false))
        & scp.exe -q -F $shopagentSsh $shopagentLocalEnv 'shopagent-linux:/home/shopagent/shopagent/deploy/.env.trade.local'
        if ($LASTEXITCODE -ne 0) { throw 'Notify configuration upload failed' }
        & ssh.exe -F $shopagentSsh -o BatchMode=yes -T shopagent-linux 'sudo -n sh /home/shopagent/shopagent/deploy/runtime.sh up -d --no-deps --force-recreate --wait --wait-timeout 120 backend-a backend-b'
        if ($LASTEXITCODE -ne 0) { throw 'Java callback configuration restart failed' }
    }
    @{public_tunnel_pid=$shopagentTunnelId;notify_url=($shopagentOrigin+'/api/payments/alipay/notify')} | ConvertTo-Json | Set-Content -LiteralPath $shopagentStatePath -Encoding UTF8
    Write-Output "Temporary payment notification origin: $shopagentOrigin"
}
Write-Output 'Website: http://127.0.0.1:8080/shopagent/home.html'
