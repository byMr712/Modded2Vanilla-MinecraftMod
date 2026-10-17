$fabricList = @(
    @{ Slug="appleskin"; Loader="fabric" },
    @{ Slug="xaeros-minimap"; Loader="fabric" },
    @{ Slug="entitytexturefeatures"; Loader="fabric" },
    @{ Slug="custom-player-models"; Loader="fabric" },
    @{ Slug="pehkui"; Loader="fabric" },
    @{ Slug="mouse-tweaks"; Loader="fabric" },
    @{ Slug="sound-physics-remastered"; Loader="fabric" },
    @{ Slug="shulkerboxtooltip"; Loader="fabric" },
    @{ Slug="zoomify"; Loader="fabric" },
    @{ Slug="cloth-config"; Loader="fabric" }
)

$neoforgeList = @(
    @{ Slug="appleskin"; Loader="neoforge" },
    @{ Slug="xaeros-minimap"; Loader="neoforge" },
    @{ Slug="entitytexturefeatures"; Loader="neoforge" },
    @{ Slug="custom-player-models"; Loader="neoforge" },
    @{ Slug="pehkui"; Loader="neoforge" },
    @{ Slug="mouse-tweaks"; Loader="neoforge" },
    @{ Slug="sound-physics-remastered"; Loader="neoforge" },
    @{ Slug="shulkerboxtooltip"; Loader="neoforge" },
    @{ Slug="badpackets"; Loader="neoforge" },
    @{ Slug="cloth-config"; Loader="neoforge" }
)

New-Item -ItemType Directory -Force -Path "test_mods/fabric" | Out-Null
New-Item -ItemType Directory -Force -Path "test_mods/neoforge" | Out-Null

$propsPath = "$PSScriptRoot/../gradle.properties"
$mcVer = "1.21"
if (Test-Path $propsPath) {
    $line = Get-Content $propsPath | Select-String "minecraft_version=" | Select-Object -First 1
    if ($line) { $mcVer = $line.Line.Split("=")[1].Trim() }
}

function Download-Mod($slug, $loader, $targetDir) {
    $url = "https://api.modrinth.com/v2/project/$slug/version?game_versions=[%22$mcVer%22]&loaders=[%22$loader%22]"
    $res = Invoke-RestMethod -Uri $url -Headers @{ "User-Agent" = "Mr712/Tester" }
    if ($res.Count -gt 0) {
        $file = $res[0].files[0]
        $outPath = Join-Path $targetDir $file.filename
        if (-not (Test-Path $outPath)) {
            Write-Output "Downloading $loader [$slug]: $($file.filename)..."
            Invoke-WebRequest -Uri $file.url -OutFile $outPath
        } else {
            Write-Output "Already exists: $($file.filename)"
        }
    } else {
        Write-Error "Could not find $slug for $loader"
    }
}

Write-Output "=== Preparing Fabric test mods ==="
foreach ($item in $fabricList) {
    Download-Mod $item.Slug $item.Loader "test_mods/fabric"
}

Write-Output "`n=== Preparing NeoForge test mods ==="
foreach ($item in $neoforgeList) {
    Download-Mod $item.Slug $item.Loader "test_mods/neoforge"
}
