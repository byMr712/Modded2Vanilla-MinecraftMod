param(
    [string]$TargetVersion = "26.3"
)

$slugCandidates = @(
    "appleskin",
    "cloth-config",
    "entitytexturefeatures",
    "entity-model-features",
    "xaeros-world-map",
    "xaeros-minimap",
    "immediatelyfast",
    "not-enough-animations",
    "yacl",
    "badpackets",
    "mouse-tweaks",
    "pehkui",
    "sound-physics-remastered",
    "shulkerboxtooltip",
    "jade",
    "modmenu"
)

function Prepare-Mods($ver, $loader) {
    $dir = "$PSScriptRoot/../test_mods/$ver/$loader"
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }

    # If already has 10 or more numbered jars, clean non-numbered or keep
    $existing = @(Get-ChildItem $dir -Filter "*.jar" -ErrorAction SilentlyContinue)
    if ($existing.Count -ge 10) {
        Write-Host "[$ver | $loader] Already has $($existing.Count) jars in $dir."
        return
    }

    Write-Host "[$ver | $loader] Collecting 10 mods for $ver..."
    $collected = @()

    foreach ($slug in $slugCandidates) {
        if ($collected.Count -ge 10) { break }
        if ($loader -eq "neoforge" -and $slug -eq "modmenu") { continue }

        $url = "https://api.modrinth.com/v2/project/$slug/version?game_versions=[%22$ver%22]&loaders=[%22$loader%22]"
        try {
            $res = Invoke-RestMethod -Uri $url -Headers @{ "User-Agent" = "Mr712/Tester" } -ErrorAction SilentlyContinue
            if ($res -and $res.Count -gt 0) {
                $f = $res[0].files | Where-Object { $_.primary } | Select-Object -First 1
                if (-not $f) { $f = $res[0].files[0] }
                $collected += @{ Slug = $slug; Filename = $f.filename; Url = $f.url }
            }
        } catch {}
    }

    # If still less than 10, search Modrinth for more
    if ($collected.Count -lt 10) {
        try {
            $searchUrl = "https://api.modrinth.com/v2/search?query=&facets=[[`"categories:$loader`"],[`"versions:$ver`"]]&limit=30"
            $searchRes = Invoke-RestMethod -Uri $searchUrl -Headers @{ "User-Agent" = "Mr712/Tester" } -ErrorAction SilentlyContinue
            $skipSlugs = @("fabric-api", "fabric-language-kotlin", "architectury-api", "sodium", "iris", "ferrite-core")
            foreach ($hit in $searchRes.hits) {
                if ($collected.Count -ge 10) { break }
                $s = $hit.slug
                if ($skipSlugs -contains $s -or ($collected.Slug -contains $s)) { continue }
                if ($loader -eq "neoforge" -and $s -eq "modmenu") { continue }
                
                $vUrl = "https://api.modrinth.com/v2/project/$s/version?game_versions=[%22$ver%22]&loaders=[%22$loader%22]"
                $vRes = Invoke-RestMethod -Uri $vUrl -Headers @{ "User-Agent" = "Mr712/Tester" } -ErrorAction SilentlyContinue
                if ($vRes -and $vRes.Count -gt 0) {
                    $f = $vRes[0].files | Where-Object { $_.primary } | Select-Object -First 1
                    if (-not $f) { $f = $vRes[0].files[0] }
                    $collected += @{ Slug = $s; Filename = $f.filename; Url = $f.url }
                }
            }
        } catch {}
    }

    # Clean existing jars to re-order cleanly 01..10
    Get-ChildItem $dir -Filter "*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

    # Ensure entitytexturefeatures is before entity-model-features
    $etf = $collected | Where-Object { $_.Slug -eq "entitytexturefeatures" }
    $emf = $collected | Where-Object { $_.Slug -eq "entity-model-features" }
    if ($etf -and $emf) {
        $etfIdx = $collected.IndexOf($etf)
        $emfIdx = $collected.IndexOf($emf)
        if ($emfIdx -lt $etfIdx) {
            $collected[$emfIdx] = $etf
            $collected[$etfIdx] = $emf
        }
    }

    $idx = 1
    foreach ($mod in $collected) {
        $prefix = "{0:D2}" -f $idx
        $destFile = Join-Path $dir "$($prefix)_$($mod.Filename)"
        Write-Host "[$ver | $loader] Downloading #$prefix : $($mod.Filename)..."
        Invoke-WebRequest -Uri $mod.Url -OutFile $destFile
        $idx++
    }
    Write-Host "[$ver | $loader] Done! $($collected.Count) mods prepared in $dir."
}

Prepare-Mods $TargetVersion "fabric"
Prepare-Mods $TargetVersion "neoforge"
