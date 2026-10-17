param(
    [string]$Server = "example-server.com",
    [int]$TimeoutSeconds = 40
)

$projectRoot = (Resolve-Path "$PSScriptRoot/..").Path
$runModsDir = "$projectRoot/run/mods"
$testModsFabric = "$projectRoot/test_mods/fabric"
$logPath = "$projectRoot/run/logs/latest.log"

if (Test-Path $logPath) {
    Remove-Item $logPath -Force -ErrorAction SilentlyContinue
}

$propsPath = "$projectRoot/gradle.properties"
$McVersion = "1.21"
if (Test-Path $propsPath) {
    $line = Get-Content $propsPath | Select-String "minecraft_version=" | Select-Object -First 1
    if ($line) {
        $McVersion = $line.Line.Split("=")[1].Trim()
    }
}

# Clean stale mods before loading test mods
if (Test-Path $runModsDir) {
    Remove-Item "$runModsDir/*.jar" -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Force -Path $runModsDir | Out-Null
}

# Automatically copy custom test mods provided in test_mods/<version>/fabric or test_mods/fabric
$modsSource = if (Test-Path "$projectRoot/test_mods/$McVersion/fabric") {
    "$projectRoot/test_mods/$McVersion/fabric"
} elseif (Test-Path $testModsFabric) {
    $testModsFabric
} else {
    $null
}

if ($modsSource) {
    $customMods = Get-ChildItem $modsSource -Filter "*.jar" -ErrorAction SilentlyContinue
    if ($customMods.Count -gt 0) {
        foreach ($m in $customMods) {
            Copy-Item $m.FullName -Destination $runModsDir -Force
        }
        Write-Output "==> Loaded $($customMods.Count) custom mod(s) from $modsSource into run/mods"
    }
}

Write-Output "==> Starting Fabric Client via Gradle targeting '$Server'..."

$isWin = ($IsWindows -or $env:OS -match "Windows")

$psi = New-Object System.Diagnostics.ProcessStartInfo
if ($isWin) {
    $psi.FileName = "cmd.exe"
    $psi.Arguments = "/c gradlew.bat runClient --args=""--quickPlayMultiplayer $Server"""
} else {
    $psi.FileName = "./gradlew"
    $psi.Arguments = "runClient --args=""--quickPlayMultiplayer $Server"""
}
$psi.WorkingDirectory = $projectRoot
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $false
$psi.RedirectStandardError = $false

$proc = [System.Diagnostics.Process]::Start($psi)
if (-not $proc) {
    Write-Error "Failed to start Gradle runClient!"
    exit 1
}

Write-Output "==> Gradle runClient wrapper PID: $($proc.Id)"

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$success = $false
$errorFound = $false
$errorDetails = ""

while ($sw.Elapsed.TotalSeconds -lt $TimeoutSeconds -and -not $proc.HasExited) {
    Start-Sleep -Milliseconds 1500
    if (Test-Path $logPath) {
        $rawLog = (Get-Content $logPath -Tail 30 -ErrorAction SilentlyContinue) -join "`n"
        if ($rawLog -match "FATAL|CrashReport|ExceptionInInitializerError|MixinApplyError") {
            $errorFound = $true
            $errorDetails = $Matches[0]
            break
        }
        if ($rawLog -match "Connecting to $Server|Captured pure vanilla block state snapshot") {
            $success = $true
            Write-Output "==> Success signature detected: $($Matches[0])"
            Start-Sleep -Seconds 3
            break
        }
    }
}

Write-Output "==> Stopping Fabric test client..."
Get-Process | Where-Object ProcessName -match 'java' | Where-Object WorkingSet64 -gt 800000000 | Stop-Process -Force -ErrorAction SilentlyContinue
if (-not $proc.HasExited) {
    try {
        $proc.Kill($true)
    } catch {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
}

if ($errorFound) {
    Write-Error "Fabric test failed with error: $errorDetails"
    exit 1
}

if ($success) {
    Write-Output "==> Fabric test PASSED successfully!"
    exit 0
} else {
    Write-Error "Fabric test timed out or connection signature not found!"
    exit 1
}
