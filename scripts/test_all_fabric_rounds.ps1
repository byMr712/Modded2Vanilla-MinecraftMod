param(
    [string]$Server = "example-server.com"
)

$projectRoot = (Resolve-Path "$PSScriptRoot/..").Path
$propsPath = "$projectRoot/gradle.properties"
$McVersion = "1.21"
if (Test-Path $propsPath) {
    $line = Get-Content $propsPath | Select-String "minecraft_version=" | Select-Object -First 1
    if ($line) {
        $McVersion = $line.Line.Split("=")[1].Trim()
    }
}

$modsSource = if (Test-Path "$projectRoot/test_mods/$McVersion/fabric") {
    "$projectRoot/test_mods/$McVersion/fabric"
} elseif (Test-Path "$projectRoot/test_mods/fabric") {
    "$projectRoot/test_mods/fabric"
} else {
    $null
}

$curated = @{
    "appleskin-fabric-mc1.21-3.0.6.jar" = "AppleSkin (HUD/Food sync)"
    "Xaeros_Minimap_25.3.2_Fabric_1.21.jar" = "Xaero's Minimap (Custom payload channels)"
    "entity_texture_features-7.2.6-1.21-fabric.jar" = "Entity Texture Features (LivingEntityRenderer layers)"
    "CustomPlayerModels-Fabric-1.21-0.6.27a.jar" = "Custom Player Models (Bone animations, player skin layers)"
    "Pehkui-3.8.3+1.14.4-1.21.jar" = "Pehkui (Scale/hitbox/reach/step height modification)"
    "MouseTweaks-fabric-mc1.21-2.26.jar" = "Mouse Tweaks (Inventory slots, container packet manipulation)"
    "sound-physics-remastered-fabric-1.21.1-1.5.1.jar" = "Sound Physics Remastered (Acoustic raytracing, sound events)"
    "shulkerboxtooltip-fabric-5.1.2+1.21.jar" = "Shulker Box Tooltip (Data component parsing, creative NBT)"
    "badpackets-fabric-0.8.2.jar" = "BadPackets (Channel manager, network packet interceptor)"
    "cloth-config-15.0.140-fabric.jar" = "Cloth Config (Dynamic configs, client screen hooks)"
}

$availableJars = if ($modsSource) { @(Get-ChildItem $modsSource -Filter "*.jar" -ErrorAction SilentlyContinue) } else { @() }
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

$runMods = "$projectRoot/run/mods"
if (-not (Test-Path $runMods)) {
    New-Item -ItemType Directory -Force -Path $runMods | Out-Null
} else {
    Remove-Item "$runMods/*" -Force -ErrorAction SilentlyContinue
}

$results = @()

function Adapt-DevJar($jarPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $zip = [System.IO.Compression.ZipFile]::Open($jarPath, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $targets = @($zip.Entries | Where-Object { $_.Name -match "\.(accesswidener|classtweaker)$" })
        foreach ($entry in $targets) {
            $sr = New-Object System.IO.StreamReader($entry.Open())
            $text = $sr.ReadToEnd()
            $sr.Close()
            if ($text -match "(accessWidener|classTweaker)\s+(v\d+)\s+official") {
                $newText = [regex]::Replace($text, "(accessWidener|classTweaker)(\s+v\d+\s+)official", '${1}${2}named')
                $entryName = $entry.FullName
                $entry.Delete()
                $newEntry = $zip.CreateEntry($entryName)
                $sw = New-Object System.IO.StreamWriter($newEntry.Open())
                $sw.Write($newText)
                $sw.Flush()
                $sw.Close()
            }
        }
    } finally {
        $zip.Dispose()
    }
}

foreach ($roundItem in $rounds) {
    $r = $roundItem.Round
    $desc = $roundItem.Desc
    Write-Output "`n========================================================"
    Write-Output ">>> STARTING FABRIC ROUND $r : $desc"
    Write-Output "========================================================"

    if ($roundItem.JarPath -and (Test-Path $roundItem.JarPath)) {
        $destJar = Join-Path $runMods (Split-Path $roundItem.JarPath -Leaf)
        Copy-Item $roundItem.JarPath -Destination $destJar -Force
        Adapt-DevJar $destJar
    }
    $loadedMods = (Get-ChildItem $runMods -Filter "*.jar" -ErrorAction SilentlyContinue).Name -join ", "
    if (-not $loadedMods) { $loadedMods = "None (pure isolation)" }
    Write-Output "Currently active mods in run/mods: $loadedMods"

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & pwsh -ExecutionPolicy Bypass -File "$PSScriptRoot/run_fabric_test.ps1" -Server $Server -TimeoutSeconds 45 -SkipModCopy
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
        Write-Error "Fabric Round $r failed! Stopping suite to inspect and fix."
        break
    }
}

Write-Output "`n========================================================"
Write-Output ">>> FABRIC TEST SUITE SUMMARY"
Write-Output "========================================================"
$results | Format-Table -AutoSize
