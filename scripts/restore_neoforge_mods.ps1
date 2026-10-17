$targetModsDir = 'C:\Users\Mr712\AppData\Roaming\ATLauncher\instances\Minecraft121withNeoForge\mods'
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
