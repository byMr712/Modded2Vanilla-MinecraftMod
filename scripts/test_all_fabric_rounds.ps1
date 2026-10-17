param(
    [string]$Server = "example-server.com"
)

$fabricMods = @(
    @{ Round=1; File="appleskin-fabric-mc1.21-3.0.6.jar"; Desc="AppleSkin (HUD/Food sync)" },
    @{ Round=2; File="Xaeros_Minimap_25.3.2_Fabric_1.21.jar"; Desc="Xaero's Minimap (Custom payload channels)" },
    @{ Round=3; File="entity_texture_features-7.2.6-1.21-fabric.jar"; Desc="Entity Texture Features (LivingEntityRenderer layers)" },
    @{ Round=4; File="CustomPlayerModels-Fabric-1.21-0.6.27a.jar"; Desc="Custom Player Models (Bone animations, player skin layers)" },
    @{ Round=5; File="Pehkui-3.8.3+1.14.4-1.21.jar"; Desc="Pehkui (Scale/hitbox/reach/step height modification)" },
    @{ Round=6; File="MouseTweaks-fabric-mc1.21-2.26.jar"; Desc="Mouse Tweaks (Inventory slots, container packet manipulation)" },
    @{ Round=7; File="sound-physics-remastered-fabric-1.21.1-1.5.1.jar"; Desc="Sound Physics Remastered (Acoustic raytracing, sound events)" },
    @{ Round=8; File="shulkerboxtooltip-fabric-5.1.2+1.21.jar"; Desc="Shulker Box Tooltip (Data component parsing, creative NBT)" },
    @{ Round=9; File="badpackets-fabric-0.8.2.jar"; Desc="BadPackets (Channel manager, network packet interceptor)" },
    @{ Round=10; File="cloth-config-15.0.140-fabric.jar"; Desc="Cloth Config (Dynamic configs, client screen hooks)" }
)

$runMods = "run/mods"
if (-not (Test-Path $runMods)) {
    New-Item -ItemType Directory -Force -Path $runMods | Out-Null
} else {
    Remove-Item "$runMods/*" -Force -ErrorAction SilentlyContinue
}

$results = @()

for ($r = 1; $r -le 10; $r++) {
    $currentMod = $fabricMods[$r - 1]
    Write-Output "`n========================================================"
    Write-Output ">>> STARTING FABRIC ROUND $r : $($currentMod.Desc)"
    Write-Output "========================================================"

    # Add mod for current round
    Copy-Item "test_mods/fabric/$($currentMod.File)" -Destination $runMods -Force
    $loadedMods = (Get-ChildItem $runMods).Name -join ", "
    Write-Output "Currently active mods in run/mods ($r total): $loadedMods"

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & pwsh -ExecutionPolicy Bypass -File "scripts/run_fabric_test.ps1" -Server $Server -TimeoutSeconds 45
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
        Write-Error "Fabric Round $r failed! Stopping suite to inspect and fix."
        break
    }
}

Write-Output "`n========================================================"
Write-Output ">>> FABRIC TEST SUITE SUMMARY"
Write-Output "========================================================"
$results | Format-Table -AutoSize
