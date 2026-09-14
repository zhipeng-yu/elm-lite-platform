# Keep runtime data outside target; use ASCII so Windows PowerShell 5.1 preserves the next line.
param([switch]$Verify)
Write-Host "Stage 2 HTTP verification requested: $($Verify.IsPresent)"
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$demoDir = Join-Path $repo '.local-demo'
$mysql = (Get-Command mysql.exe).Source
$mysqld = (Get-Command mysqld.exe).Source
$java = (Get-Command java.exe).Source
$node = (Get-Command node.exe).Source
$children = @()

foreach ($port in @(13317, 18081, 5180)) {
    $probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
    try { $probe.Start() } finally { $probe.Stop() }
}
$legacyDir = Join-Path $repo 'backend/target/local-demo'
if (Test-Path -LiteralPath $legacyDir) {
    if (Test-Path -LiteralPath $demoDir) { throw 'Both .local-demo and backend/target/local-demo exist; preserve both and resolve which database to use before starting.' }
    $resolvedLegacy = (Resolve-Path -LiteralPath $legacyDir).Path
    $expectedLegacy = [IO.Path]::GetFullPath((Join-Path $repo 'backend/target/local-demo'))
    if ($resolvedLegacy -ne $expectedLegacy -or ((Get-Item -LiteralPath $legacyDir).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Refusing to move an unexpected demo path.' }
    Move-Item -LiteralPath $resolvedLegacy -Destination ([IO.Path]::GetFullPath($demoDir))
    Write-Host 'Existing demo database and logs moved to .local-demo.'
}
New-Item -ItemType Directory -Force $demoDir | Out-Null

Push-Location (Join-Path $repo 'backend')
try {
    # Windows PowerShell 5.1 turns redirected native stderr into error records.
    try {
        $ErrorActionPreference = 'Continue'
        & .\mvnw.cmd verify *> (Join-Path $demoDir 'verify.log')
    } finally { $ErrorActionPreference = 'Stop' }
    if ($LASTEXITCODE -ne 0) { throw "Backend verification failed: $demoDir/verify.log" }
} finally { Pop-Location }

if (!(Test-Path (Join-Path $repo 'front-end/node_modules/vite/bin/vite.js'))) {
    Push-Location (Join-Path $repo 'front-end')
    try { & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' } } finally { Pop-Location }
}

$dataDir = Join-Path $demoDir 'mysql'
if (!(Test-Path $dataDir)) {
    try {
        $ErrorActionPreference = 'Continue'
        & $mysqld --no-defaults --initialize-insecure "--datadir=$dataDir" *> (Join-Path $demoDir 'mysql-init.log')
    } finally { $ErrorActionPreference = 'Stop' }
    if ($LASTEXITCODE -ne 0) { throw 'MySQL initialization failed; see mysql-init.log' }
}

try {
    $children += Start-Process $mysqld -ArgumentList @('--no-defaults', "--datadir=`"$dataDir`"", '--port=13317', '--bind-address=127.0.0.1', '--mysqlx=0', '--console') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $demoDir 'mysql.log') -RedirectStandardError (Join-Path $demoDir 'mysql-error.log')
    $dbArgs = @('--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port=13317', '--user=root', '--default-character-set=utf8mb4')
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        try { & $mysql @dbArgs -e 'SELECT 1' *> $null; $ready = $LASTEXITCODE -eq 0 } catch { $ready = $false }
        if ($ready) { break }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw 'MySQL did not start; see mysql-error.log' }
    & $mysql @dbArgs -e 'CREATE DATABASE IF NOT EXISTS elm_lite CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE -ne 0) { throw 'Database creation failed' }
    if (!(Test-Path (Join-Path $demoDir 'initialized'))) {
        foreach ($sql in @('database/migration/V1__create_initial_schema.sql', 'database/migration/V2__add_username_to_users.sql', 'database/init/V1__seed_data.sql')) {
            $sourcePath = (Join-Path $repo $sql).Replace('\', '/')
            & $mysql @dbArgs elm_lite -e "source $sourcePath"
            if ($LASTEXITCODE -ne 0) { throw "Migration failed: $sql" }
        }
        New-Item -ItemType File (Join-Path $demoDir 'initialized') | Out-Null
    }
    $migrations = Get-ChildItem (Join-Path $repo 'database/migration/V*__*.sql') |
        Where-Object { $_.BaseName -notmatch '^V[12]__' } |
        Sort-Object { [int]($_.BaseName -replace '^V(\d+)__.*$', '$1') }
    foreach ($migration in $migrations) {
        $marker = Join-Path $demoDir "migration-$($migration.BaseName).applied"
        if (!(Test-Path $marker)) {
            $sourcePath = $migration.FullName.Replace('\', '/')
            & $mysql @dbArgs elm_lite -e "source $sourcePath"
            if ($LASTEXITCODE -ne 0) { throw "Migration failed: $($migration.Name)" }
            New-Item -ItemType File $marker | Out-Null
        }
    }
    & python (Join-Path $repo 'scripts/seed-demo.py')
    if ($LASTEXITCODE -ne 0) { throw 'Demo catalog import failed' }
    $env:DB_URL = 'jdbc:mysql://127.0.0.1:13317/elm_lite?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
    $env:DB_USERNAME = 'root'
    $env:DB_PASSWORD = ''
    $bytes = New-Object byte[] 48
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
    # Run a copy so rebuilding target does not replace a running JVM's classes on Windows.
    $jar = Join-Path $demoDir 'backend-demo.jar'
    Copy-Item -LiteralPath (Join-Path $repo 'backend/target/elm-lite-platform-0.0.1-SNAPSHOT.jar') -Destination $jar
    $children += Start-Process $java -ArgumentList @('-jar', "`"$jar`"", '--server.address=127.0.0.1', '--server.port=18081', '--spring.datasource.password=', '--logging.level.root=INFO', '--logging.level.org.springframework=INFO') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $demoDir 'backend.log') -RedirectStandardError (Join-Path $demoDir 'backend-error.log')
    $env:API_PROXY_TARGET = 'http://127.0.0.1:18081'
    $env:VITE_USE_MOCK = 'false'
    $vite = Join-Path $repo 'front-end/node_modules/vite/bin/vite.js'
    $children += Start-Process $node -ArgumentList @("`"$vite`"", '--host', '127.0.0.1', '--port', '5180', '--strictPort') -WorkingDirectory (Join-Path $repo 'front-end') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $demoDir 'frontend.log') -RedirectStandardError (Join-Path $demoDir 'frontend-error.log')
    $ready = $false
    for ($i = 0; $i -lt 45; $i++) {
        try {
            $response = Invoke-RestMethod 'http://127.0.0.1:5180/api/v1/shops' -TimeoutSec 2
            if ($response.code -eq 0) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 1 }
    }
    if (!$ready) { throw "Demo startup failed; see logs in $demoDir" }
    Write-Host 'Demo ready: http://127.0.0.1:5180'
    if ($Verify) {
        & python (Join-Path $repo 'scripts/stage2-smoke.py')
        if ($LASTEXITCODE -ne 0) { throw 'Stage 2 verification failed.' }
    }
    Write-Host 'Use the UI to register a new user and merchant. Data survives restart; sign in again after restarting.'
    Read-Host 'Press Enter to stop the demo' | Out-Null
} finally {
    for ($i = $children.Count - 1; $i -ge 1; $i--) {
        try { & taskkill.exe /PID $children[$i].Id /T /F 2>$null | Out-Null } catch { }
    }
    if ($children.Count -gt 0) {
        & (Join-Path (Split-Path $mysql) 'mysqladmin.exe') @dbArgs shutdown
    }
}
