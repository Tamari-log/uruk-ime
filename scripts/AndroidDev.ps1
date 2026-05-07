# uruk-ime (Uruk IME) / Android 実機開発まわり一式
# - SDK 解決（ANDROID_HOME / local.properties、末尾空白の除去）
# - platform-tools が無ければ sdkmanager で導入（要 cmdline-tools）
# - 必要なら SDK ライセンス一括承認（y を複数行パイプ）
# - PATH 先頭に platform-tools を追加
# - adb デバイス一覧
# - gradlew installDebug（既定）で実機へインストール
#
# 使い方（このシェルに PATH が残る）:
#   . .\scripts\AndroidDev.ps1
#
# PATH だけ:
#   . .\scripts\AndroidDev.ps1 -PathOnly
#
# インストールはせず PATH+ツールだけ:
#   . .\scripts\AndroidDev.ps1 -NoInstall

[CmdletBinding()]
param(
    [switch]$PathOnly,
    [switch]$NoInstall,
    [switch]$NoEnsurePlatformTools,
    [switch]$SkipLicenses,
    [switch]$RestartAdb,
    [string]$GradleTask = "installDebug"
)

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) {
    $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
}
$RepoRoot = Split-Path -Parent $ScriptDir

function Resolve-SdkDir {
    if ($env:ANDROID_HOME) { return $env:ANDROID_HOME.Trim() }
    if ($env:ANDROID_SDK_ROOT) { return $env:ANDROID_SDK_ROOT.Trim() }
    $propsPath = Join-Path $RepoRoot "local.properties"
    if (-not (Test-Path $propsPath)) { return $null }
    foreach ($line in Get-Content $propsPath) {
        if ($line -match '^\s*sdk\.dir\s*=\s*(.+)\s*$') {
            $raw = $Matches[1].Trim()
            return ($raw -replace '\\:', ':' -replace '\\\\', '\').Trim()
        }
    }
    return $null
}

function Get-SdkManagerBat {
    param([Parameter(Mandatory)][string]$SdkRoot)
    $latest = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
    if (Test-Path $latest) { return $latest }
    $ctRoot = Join-Path $SdkRoot "cmdline-tools"
    if (Test-Path $ctRoot) {
        foreach ($d in (Get-ChildItem $ctRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending)) {
            $bat = Join-Path $d.FullName "bin\sdkmanager.bat"
            if (Test-Path $bat) { return $bat }
        }
    }
    $legacy = Join-Path $SdkRoot "tools\bin\sdkmanager.bat"
    if (Test-Path $legacy) { return $legacy }
    return $null
}

function Approve-AndroidSdkLicenses {
    param(
        [Parameter(Mandatory)][string]$SdkManagerBat,
        [Parameter(Mandatory)][string]$SdkRoot
    )
    Write-Host "SDK licenses (auto y)..." -ForegroundColor Cyan
    $ys = 1..80 | ForEach-Object { "y" }
    $null = $ys | & $SdkManagerBat --sdk_root=$SdkRoot --licenses 2>&1
}

function Install-PlatformTools {
    param(
        [Parameter(Mandatory)][string]$SdkManagerBat,
        [Parameter(Mandatory)][string]$SdkRoot
    )
    Write-Host "Installing platform-tools via sdkmanager..." -ForegroundColor Cyan
    & $SdkManagerBat --sdk_root=$SdkRoot "platform-tools"
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "sdkmanager platform-tools exited with $LASTEXITCODE"
        return $false
    }
    return $true
}

function Add-PlatformToolsToPath {
    param(
        [Parameter(Mandatory)][string]$SdkRoot
    )
    $pt = Join-Path $SdkRoot "platform-tools"
    if (-not (Test-Path $pt)) {
        Write-Warning "platform-tools directory not found: $pt"
        return $false
    }
    $adb = Join-Path $pt "adb.exe"
    if (-not (Test-Path $adb)) {
        Write-Warning "adb.exe not found under $pt"
        return $false
    }
    $env:ANDROID_HOME = $SdkRoot.TrimEnd('\', '/')
    $sep = [System.IO.Path]::PathSeparator
    if ($env:PATH -notlike "*${pt}*") {
        $env:PATH = "$pt$sep$env:PATH"
    }
    Write-Host "PATH ok: $pt" -ForegroundColor Green
    return $true
}

function Invoke-EnsurePlatformTools {
    param(
        [Parameter(Mandatory)][string]$SdkRoot,
        [bool]$AutoLicenses = $true
    )
    $pt = Join-Path $SdkRoot "platform-tools\adb.exe"
    if (Test-Path $pt) { return $true }

    $sm = Get-SdkManagerBat -SdkRoot $SdkRoot
    if (-not $sm) {
        Write-Warning @"
platform-tools が見つかりません。Android Studio の SDK Manager で Command-line Tools を入れるか、次を参照してください。
https://developer.android.com/studio#command-line-tools-only
SDK root: $SdkRoot
"@
        return $false
    }

    if ($AutoLicenses -and -not $SkipLicenses) {
        Approve-AndroidSdkLicenses -SdkManagerBat $sm -SdkRoot $SdkRoot
    }

    if (-not (Install-PlatformTools -SdkManagerBat $sm -SdkRoot $SdkRoot)) {
        return $false
    }
    return $true
}

Write-Host "=== AndroidDev (uruk-ime) ===" -ForegroundColor Magenta

$sdk = Resolve-SdkDir
if (-not $sdk -or -not (Test-Path $sdk)) {
    Write-Warning "Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties."
    return
}

if (-not $PathOnly -and -not $NoEnsurePlatformTools) {
    $null = Invoke-EnsurePlatformTools -SdkRoot $sdk -AutoLicenses (-not $SkipLicenses)
}

if (-not (Add-PlatformToolsToPath -SdkRoot $sdk)) {
    if ($PathOnly) {
        return
    }
    Write-Warning "adb not available; skipping device steps."
    return
}

if ($RestartAdb) {
    Write-Host "Restarting adb..." -ForegroundColor Cyan
    & adb kill-server 2>$null
    & adb start-server 2>$null
}

Write-Host "`n--- adb devices ---" -ForegroundColor DarkGray
& adb devices -l
Write-Host ""

if ($PathOnly -or $NoInstall) {
    Write-Host "Done (PathOnly or NoInstall)." -ForegroundColor Green
    return
}

$gw = Join-Path $RepoRoot "gradlew.bat"
if (-not (Test-Path $gw)) {
    Write-Warning "gradlew.bat not found: $gw"
    return
}

Write-Host "--- gradlew $GradleTask ---" -ForegroundColor Cyan
Push-Location $RepoRoot
try {
    & .\gradlew.bat $GradleTask
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Gradle exited with code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

Write-Host "`n--- adb devices (after install) ---" -ForegroundColor DarkGray
& adb devices -l
Write-Host "`nAndroidDev finished." -ForegroundColor Green
