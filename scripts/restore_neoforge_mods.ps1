$projectRoot = (Resolve-Path "$PSScriptRoot/..").Path
$userHome = [System.Environment]::GetFolderPath([System.Environment+SpecialFolder]::UserProfile)
$usersDir = if ($env:ATLAUNCHER_DIR) { $env:ATLAUNCHER_DIR } elseif ($env:APPDATA -and (Test-Path "$env:APPDATA\ATLauncher")) { "$env:APPDATA\ATLauncher" } else { "$userHome/.local/share/atlauncher" }
$propsPath = "$projectRoot/gradle.properties"
$McVersion = "1.21"
if (Test-Path $propsPath) {
    $line = Get-Content $propsPath | Select-String "minecraft_version=" | Select-Object -First 1
    if ($line) { $McVersion = $line.Line.Split("=")[1].Trim() }
}
$instanceName = "Minecraft$($McVersion.Replace('.', ''))withNeoForge"
$targetModsDir = Join-Path $usersDir "instances/$instanceName/mods"
$testMods = @(
    'entity_texture_features-7.2.6-1.21-neoforge.jar',
    'CustomPlayerModels-1.21-0.6.27a.jar',
    'Pehkui-3.8.3+1.21-neoforge.jar',
    'MouseTweaks-neoforge-mc1.21-2.26.1.jar',
    'sound-physics-remastered-neoforge-1.21.1-1.5.1.jar',
    'shulkerboxtooltip-neoforge-5.1.2+1.21.jar',
    'badpackets-neo-0.8.2.jar',
    'cloth-config-15.0.140-neoforge.jar'
)
foreach ($m in $testMods) {
    $p = Join-Path $targetModsDir $m
    if (Test-Path $p) {
        Remove-Item $p -Force -ErrorAction SilentlyContinue
    }
}
Write-Output "Restored NeoForge instance mods:"
Get-ChildItem $targetModsDir | Select-Object Name
