# Uruk IME (uruk-ime) 用: logcat をファイルに保存し、関連行だけ別ファイルにも書く。
# 前提: USB デバッグが許可され、`adb devices` が device であること。
#
#   . .\scripts\AndroidDev.ps1 -PathOnly -NoEnsurePlatformTools
#   .\scripts\Capture-UrukImeLogcat.ps1
#
# 再現直前にバッファを空にする:
#   .\scripts\Capture-UrukImeLogcat.ps1 -ClearFirst
# その後フォームで IME を落とし、もう一度実行してダンプ。

param(
    [switch]$ClearFirst,
    [int]$TailLines = 3000
)

$ScriptDir = $PSScriptRoot
$RepoRoot = Split-Path -Parent $ScriptDir
$buildDir = Join-Path $RepoRoot "build"
$null = New-Item -ItemType Directory -Force -Path $buildDir

. (Join-Path $ScriptDir "AndroidDev.ps1") -PathOnly -NoEnsurePlatformTools

$deviceOk = adb devices 2>&1 | Where-Object { $_ -match "`tdevice$" }
if (-not $deviceOk) {
    Write-Warning "adb で認識されたデバイスがありません（unauthorized / offline の可能性）。`n$(adb devices 2>&1 | Out-String)"
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$fullLog = Join-Path $buildDir "enmerkar-ime-logcat-full-$stamp.txt"
$filtLog = Join-Path $buildDir "enmerkar-ime-logcat-filtered-$stamp.txt"

if ($ClearFirst) {
    Write-Host "Clearing logcat buffer..." -ForegroundColor Cyan
    adb logcat -c 2>&1 | Out-Null
}

Write-Host "Dumping logcat (-t $TailLines)..." -ForegroundColor Cyan
# main + system + crash (crash バッファは端末によっては空)
$raw = adb logcat -d -b main -b system -b crash -t $TailLines 2>&1
$raw | Out-File -FilePath $fullLog -Encoding utf8

$patterns = @(
    "belleval\.enmerkar", "com\.belleval\.enmerkar\.type", "UrukIme", "cuneiform", "Cuneiform", "FATAL EXCEPTION", "AndroidRuntime",
    "am_crash", "ActivityManager.*Process.*enmerkar", "InputMethod", "RemoteException",
    "IllegalState", "IllegalArgument", "NullPointer", "Compose"
)

$filtered = $raw | Where-Object {
    $line = $_
    foreach ($p in $patterns) {
        if ($line -match $p) { return $true }
    }
    return $false
}
$filtered | Out-File -FilePath $filtLog -Encoding utf8

Write-Host "Full log:  $fullLog" -ForegroundColor Green
Write-Host "Filtered: $filtLog" -ForegroundColor Green
