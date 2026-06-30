#Requires -Version 5
<#
.SYNOPSIS
    Build, sign, and publish a Life Simulator Android release.

.DESCRIPTION
    1. Bumps versionCode / versionName in app/build.gradle.kts.
    2. Builds a signed release APK with the Gradle wrapper.
    3. Creates a GitHub Release with the APK on the public releases repo.
    4. Updates version.json in the releases repo so installed apps can offer the update.

    The version bump in THIS repo is left uncommitted for review and commit.

.EXAMPLE
    .\release.ps1 -VersionName 0.2.0 -VersionCode 2 -Notes "New actions and balance changes."
#>
param(
    [Parameter(Mandatory = $true)][string]$VersionName,
    [Parameter(Mandatory = $true)][int]$VersionCode,
    [string]$Notes = "Bug fixes and improvements."
)

$ErrorActionPreference = 'Stop'
$proj = $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
}
if (-not $env:ANDROID_HOME) {
    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
}

$releasesRepo = 'AzizjonKasimov/life-simulator-app-releases'
$releasesDir = Join-Path (Split-Path $proj -Parent) 'life-simulator-app-releases'
$apkName = "LifeSimulator-$VersionName.apk"
$apkUrl = "https://github.com/$releasesRepo/releases/download/v$VersionName/$apkName"

foreach ($required in @('release.keystore', 'keystore.properties')) {
    if (-not (Test-Path (Join-Path $proj $required))) {
        throw "$required is missing. Create local signing files first; see README.md."
    }
}

gh repo view $releasesRepo *> $null
if ($LASTEXITCODE -ne 0) {
    throw "GitHub releases repo $releasesRepo was not found."
}

gh release view "v$VersionName" --repo $releasesRepo *> $null
if ($LASTEXITCODE -eq 0) {
    throw "Release v$VersionName already exists in $releasesRepo."
}

Write-Host "==> Bumping version to $VersionName (code $VersionCode)" -ForegroundColor Cyan
$gradleFile = Join-Path $proj 'app\build.gradle.kts'
$content = Get-Content $gradleFile -Raw
$content = [regex]::Replace($content, 'versionCode = \d+', "versionCode = $VersionCode")
$content = [regex]::Replace($content, 'versionName = "[^"]*"', "versionName = `"$VersionName`"")
Set-Content -Path $gradleFile -Value $content -NoNewline

Write-Host "==> Building signed release APK" -ForegroundColor Cyan
& (Join-Path $proj 'gradlew.bat') -p $proj assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}

$built = Join-Path $proj 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path $built)) {
    throw "Signed APK was not produced at $built."
}
$named = Join-Path $proj "app\build\outputs\apk\release\$apkName"
Copy-Item $built $named -Force
Copy-Item $named (Join-Path $proj $apkName) -Force
Copy-Item $named (Join-Path $proj 'LifeSimulator-latest.apk') -Force

Write-Host "==> Publishing GitHub release v$VersionName" -ForegroundColor Cyan
gh release create "v$VersionName" $named --repo $releasesRepo --title "v$VersionName" --notes $Notes --latest
if ($LASTEXITCODE -ne 0) {
    throw "gh release create failed."
}

Write-Host "==> Updating version.json in the releases repo" -ForegroundColor Cyan
if (-not (Test-Path (Join-Path $releasesDir '.git'))) {
    gh repo clone $releasesRepo $releasesDir
} else {
    git -C $releasesDir pull --quiet
}

$json = [ordered]@{
    versionCode = $VersionCode
    versionName = $VersionName
    apkUrl = $apkUrl
    notes = $Notes
} | ConvertTo-Json
Set-Content -Path (Join-Path $releasesDir 'version.json') -Value $json
if (git -C $releasesDir status --short version.json) {
    git -C $releasesDir add version.json
    git -C $releasesDir commit -m "Release v$VersionName (code $VersionCode)" | Out-Null
    git -C $releasesDir push --quiet
}

Write-Host ""
Write-Host "Done. v$VersionName is published; installed apps will offer the update on next launch." -ForegroundColor Green
Write-Host "Now review and commit the source repo version bump:" -ForegroundColor DarkGray
Write-Host "  git add app/build.gradle.kts; git commit -m `"Release v$VersionName`"" -ForegroundColor DarkGray
