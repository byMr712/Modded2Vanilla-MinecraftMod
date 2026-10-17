param(
    [string]$Server = "hub.opblocks.com",
    [int]$TimeoutSeconds = 40
)

$runDir = "$PSScriptRoot/.."
$logPath = "$runDir/run/logs/latest.log"

if (Test-Path $logPath) {
    Remove-Item $logPath -Force -ErrorAction SilentlyContinue
}

Write-Output "==> Starting Fabric Client via Gradle targeting '$Server'..."

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "cmd.exe"
$psi.Arguments = "/c gradlew.bat runClient --args=""--quickPlayMultiplayer $Server"""
$psi.WorkingDirectory = (Resolve-Path $runDir).Path
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
