$neoforgeMods = @(
    @{ Round=1; File="appleskin-neoforge-mc1.21-3.0.9.jar"; Desc="AppleSkin (HUD/Food sync)" },
    @{ Round=2; File="xaerominimap-neoforge-1.21.1-26.6.0.jar"; Desc="Xaero's Minimap (Custom payload channels)" },
    @{ Round=3; File="entity_texture_features-7.2.6-1.21-neoforge.jar"; Desc="Entity Texture Features (LivingEntityRenderer layers)" },
    @{ Round=4; File="CustomPlayerModels-1.21-0.6.27a.jar"; Desc="Custom Player Models (Bone animations, player skin layers)" },
    @{ Round=5; File="Pehkui-3.8.3+1.21-neoforge.jar"; Desc="Pehkui (Scale/hitbox/reach/step height modification)" },
    @{ Round=6; File="MouseTweaks-neoforge-mc1.21-2.26.1.jar"; Desc="Mouse Tweaks (Inventory slots, container packet manipulation)" },
    @{ Round=7; File="sound-physics-remastered-neoforge-1.21.1-1.5.1.jar"; Desc="Sound Physics Remastered (Acoustic raytracing, sound events)" },
    @{ Round=8; File="shulkerboxtooltip-neoforge-5.1.2+1.21.jar"; Desc="Shulker Box Tooltip (Data component parsing, creative NBT)" },
    @{ Round=9; File="badpackets-neo-0.8.2.jar"; Desc="BadPackets (Channel manager, network packet interceptor)" },
    @{ Round=10; File="cloth-config-15.0.140-neoforge.jar"; Desc="Cloth Config (Dynamic configs, client screen hooks)" }
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
$targetModsDir = Join-Path $usersDir "instances/$instanceName/mods"

# Ensure MrModded2Vanilla NeoForge jar is fresh
Write-Output "==> Building fresh MrModded2Vanilla NeoForge jar..."
if ($isWin) {
    & cmd.exe /c gradlew.bat jarNeoForge copyToLauncherNeoForge
} else {
    & ./gradlew jarNeoForge copyToLauncherNeoForge
}

# Remove test mods before starting progressive rounds
$allTestFileNames = $neoforgeMods | ForEach-Object { $_.File }
foreach ($fn in $allTestFileNames) {
    $existing = Join-Path $targetModsDir $fn
    if (Test-Path $existing) {
        Remove-Item $existing -Force -ErrorAction SilentlyContinue
    }
}

$results = @()

for ($r = 1; $r -le 10; $r++) {
    $currentMod = $neoforgeMods[$r - 1]
    Write-Output "`n========================================================"
    Write-Output ">>> STARTING NEOFORGE ROUND $r : $($currentMod.Desc)"
    Write-Output "========================================================"

    # Add mod for current round
    Copy-Item "test_mods/neoforge/$($currentMod.File)" -Destination $targetModsDir -Force
    $activeCount = (Get-ChildItem $targetModsDir -Filter "*.jar").Count
    Write-Output "Currently active mods in NeoForge mods folder ($activeCount total):"
    Get-ChildItem $targetModsDir -Filter "*.jar" | ForEach-Object { Write-Output "  - $($_.Name)" }

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & pwsh -ExecutionPolicy Bypass -File "scripts/run_neoforge_test.ps1" -Server "hub.opblocks.com" -TimeoutSeconds 45
    $exitCode = $LASTEXITCODE
    $sw.Stop()

    $status = if ($exitCode -eq 0) { "PASSED" } else { "FAILED" }
    Write-Output ">>> ROUND $r RESULT: $status (Duration: $([int]$sw.Elapsed.TotalSeconds)s)"

    $results += [PSCustomObject]@{
        Round = $r
        Mod = $currentMod.Desc
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
