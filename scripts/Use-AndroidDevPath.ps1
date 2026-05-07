# Back-compat: dot-source 用ラッパー（本体は AndroidDev.ps1）
#   . .\scripts\Use-AndroidDevPath.ps1
#   . .\scripts\Use-AndroidDevPath.ps1 -PathOnly
#
param(
    [switch]$PathOnly,
    [switch]$NoInstall,
    [switch]$NoEnsurePlatformTools,
    [switch]$SkipLicenses,
    [switch]$RestartAdb,
    [string]$GradleTask = "installDebug"
)

. (Join-Path $PSScriptRoot "AndroidDev.ps1") @PSBoundParameters
