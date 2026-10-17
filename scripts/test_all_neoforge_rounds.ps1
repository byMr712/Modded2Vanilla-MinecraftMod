param(
    [string]$Server = "example-server.com"
)

$projectRoot = (Resolve-Path "$PSScriptRoot/..").Path
$isWin = ($IsWindows -or $env:OS -match "Windows")
$userHome = [System.Environment]::GetFolderPath([System.Environment+SpecialFolder]::UserProfile)

$usersDir = if ($env:ATLAUNCHER_DIR) {
    $env:ATLAUNCHER_DIR
} elseif ($env:APPDATA -and (Test-Path "$env:APPDATA\ATLauncher")) {
    "$env:APPDATA\ATLauncher"
} elseif (Test-Path "$userHome/Library/Application Support/ATLauncher") {
    "$userHome/Library/Application Support/ATLauncher"
} elseif (Test-Path "$userHome/.local/share/atlauncher") {
    "$userHome/.local/share/atlauncher"
} elseif ($env:APPDATA) {
    "$env:APPDATA\ATLauncher"
} else {
    "$userHome/.local/share/atlauncher"
}

$propsPath = "$projectRoot/gradle.properties"
$McVersion = "1.21"
if (Test-Path $propsPath) {
    $line = Get-Content $propsPath | Select-String "minecraft_version=" | Select-Object -First 1
    if ($line) {
        $McVersion = $line.Line.Split("=")[1].Trim()
    }
}
$instanceName = "Minecraft$($McVersion.Replace('.', ''))withNeoForge"
$instanceDir = Join-Path $usersDir "instances/$instanceName"

# If exact named instance doesn't exist, search for any instance matching NeoForge and version
if (-not (Test-Path $instanceDir)) {
    $instancesRoot = Join-Path $usersDir "instances"
    if (Test-Path $instancesRoot) {
        $found = Get-ChildItem $instancesRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match "NeoForge" -and $_.Name -match [regex]::Escape($McVersion.Replace('.', '')) } |
            Select-Object -First 1
        if ($found) {
            $instanceDir = $found.FullName
            $instanceName = $found.Name
        }
    }
}

if (-not (Test-Path $instanceDir)) {
    Write-Error "NeoForge instance for Minecraft $McVersion was not found at '$instanceDir'. Please ensure an instance named '$instanceName' exists in ATLauncher, or set the ATLAUNCHER_DIR environment variable."
    exit 1
}

$targetModsDir = Join-Path $instanceDir "mods"

# Ensure MrModded2Vanilla NeoForge jar is fresh
Write-Output "==> Building fresh MrModded2Vanilla NeoForge jar..."
if ($isWin) {
    & cmd.exe /c gradlew.bat jarNeoForge copyToLauncherNeoForge
} else {
    & ./gradlew jarNeoForge copyToLauncherNeoForge
}

# Clean stale third-party mods, keeping MrModded2Vanilla
if (Test-Path $targetModsDir) {
    Get-ChildItem "$targetModsDir/*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "MrModded2Vanilla" } |
        Remove-Item -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Force -Path $targetModsDir | Out-Null
}

$modsSourceNeo = if (Test-Path "$projectRoot/test_mods/$McVersion/neoforge") {
    "$projectRoot/test_mods/$McVersion/neoforge"
} elseif (Test-Path "$projectRoot/test_mods/neoforge") {
    "$projectRoot/test_mods/neoforge"
} else {
    $null
}

$curated = @{
    "appleskin-neoforge-mc1.21-3.0.9.jar" = "AppleSkin (HUD/Food sync)"
    "xaerominimap-neoforge-1.21.1-26.6.0.jar" = "Xaero's Minimap (Custom payload channels)"
    "entity_texture_features-7.2.6-1.21-neoforge.jar" = "Entity Texture Features (LivingEntityRenderer layers)"
    "CustomPlayerModels-1.21-0.6.27a.jar" = "Custom Player Models (Bone animations, player skin layers)"
    "Pehkui-3.8.3+1.21-neoforge.jar" = "Pehkui (Scale/hitbox/reach/step height modification)"
    "MouseTweaks-neoforge-mc1.21-2.26.1.jar" = "Mouse Tweaks (Inventory slots, container packet manipulation)"
    "sound-physics-remastered-neoforge-1.21.1-1.5.1.jar" = "Sound Physics Remastered (Acoustic raytracing, sound events)"
    "shulkerboxtooltip-neoforge-5.1.2+1.21.jar" = "Shulker Box Tooltip (Data component parsing, creative NBT)"
    "badpackets-neo-0.8.2.jar" = "BadPackets (Channel manager, network packet interceptor)"
    "cloth-config-15.0.140-neoforge.jar" = "Cloth Config (Dynamic configs, client screen hooks)"
}

$availableJars = if ($modsSourceNeo) { @(Get-ChildItem $modsSourceNeo -Filter "*.jar" -ErrorAction SilentlyContinue) } else { @() }
$rounds = @()

if ($availableJars.Count -gt 0) {
    $count = [Math]::Min($availableJars.Count, 10)
    for ($i = 0; $i -lt $count; $i++) {
        $jar = $availableJars[$i]
        $desc = if ($curated.ContainsKey($jar.Name)) { $curated[$jar.Name] } else { $jar.BaseName }
        $rounds += @{ Round = ($i + 1); JarPath = $jar.FullName; Desc = $desc }
    }
} else {
    $rounds += @{ Round = 1; JarPath = ""; Desc = "MrModded2Vanilla Baseline Isolation" }
}

$results = @()

foreach ($roundItem in $rounds) {
    $r = $roundItem.Round
    $desc = $roundItem.Desc
    Write-Output "`n========================================================"
    Write-Output ">>> STARTING NEOFORGE ROUND $r : $desc"
    Write-Output "========================================================"

    if ($roundItem.JarPath -and (Test-Path $roundItem.JarPath)) {
        Copy-Item $roundItem.JarPath -Destination $targetModsDir -Force
    }
    $activeCount = (Get-ChildItem $targetModsDir -Filter "*.jar" -ErrorAction SilentlyContinue).Count
    Write-Output "Currently active mods in NeoForge mods folder ($activeCount total):"
    Get-ChildItem $targetModsDir -Filter "*.jar" -ErrorAction SilentlyContinue | ForEach-Object { Write-Output "  - $($_.Name)" }

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & pwsh -ExecutionPolicy Bypass -File "$PSScriptRoot/run_neoforge_test.ps1" -Server $Server -TimeoutSeconds 45 -SkipModCopy
    $exitCode = $LASTEXITCODE
    $sw.Stop()

    $status = if ($exitCode -eq 0) { "PASSED" } else { "FAILED" }
    Write-Output ">>> ROUND $r RESULT: $status (Duration: $([int]$sw.Elapsed.TotalSeconds)s)"

    $results += [PSCustomObject]@{
        Round = $r
        Mod = $desc
        Status = $status
        Duration = "$([int]$sw.Elapsed.TotalSeconds)s"
    }

    if ($exitCode -ne 0) {
        Write-Error "NeoForge Round $r failed! Stopping suite to inspect and fix."
        break
    }
}

Write-Output "`n========================================================"
Write-Output ">>> NEOFORGE TEST SUITE SUMMARY"
Write-Output "========================================================"
$results | Format-Table -AutoSize
