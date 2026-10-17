param(
    [string]$Server = "hub.opblocks.com",
    [int]$TimeoutSeconds = 40
)

$usersDir = "C:\Users\Mr712\AppData\Roaming\ATLauncher"
$logPath = "$usersDir\instances\Minecraft121withNeoForge\logs\latest.log"

if (Test-Path $logPath) {
    Remove-Item $logPath -Force -ErrorAction SilentlyContinue
}

$rawCmd = Get-Content "$PSScriptRoot/neoforge_cmd.txt" -Raw
$marker = ": ["
$startIdx = $rawCmd.IndexOf($marker)
$endIdx = $rawCmd.LastIndexOf(']')
if ($startIdx -lt 0 -or $endIdx -le $startIdx) {
    Write-Error "Could not parse NeoForge command template!"
    exit 1
}

$argString = $rawCmd.Substring($startIdx + $marker.Length, $endIdx - ($startIdx + $marker.Length))
$parts = $argString -split ', '
$resolvedArgs = @()
foreach ($p in $parts) {
    $p = $p.Trim()
    $p = $p.Replace("USERSDIR", $usersDir)
    $resolvedArgs += $p
}

$javaExe = $resolvedArgs[0]
if (-not $javaExe.EndsWith(".exe")) {
    $javaExe = "$javaExe.exe"
}
$javaExe = $javaExe.Replace("javaw.exe", "java.exe")

$remainingArgs = $resolvedArgs[1..($resolvedArgs.Count - 1)]

# QuickPlay parameters
if ($Server) {
    $remainingArgs += "--quickPlayMultiplayer"
    $remainingArgs += $Server
}

Write-Output "==> Starting NeoForge targeting '$Server'..."

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaExe
foreach ($a in $remainingArgs) {
    $psi.ArgumentList.Add($a)
}
$psi.WorkingDirectory = "$usersDir\instances\Minecraft121withNeoForge"
$psi.UseShellExecute = $false
# Do NOT redirect stdout/stderr without async reader to prevent OS pipe deadlocks
$psi.RedirectStandardOutput = $false
$psi.RedirectStandardError = $false

$proc = [System.Diagnostics.Process]::Start($psi)
if (-not $proc) {
    Write-Error "Failed to start NeoForge process!"
    exit 1
}

Write-Output "==> NeoForge PID: $($proc.Id)"

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

Write-Output "==> Stopping NeoForge test client..."
if (-not $proc.HasExited) {
    try {
        $proc.Kill($true)
    } catch {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
}

if ($errorFound) {
    Write-Error "NeoForge test failed with error: $errorDetails"
    exit 1
}

if ($success) {
    Write-Output "==> NeoForge test PASSED successfully!"
    exit 0
} else {
    Write-Error "NeoForge test timed out or connection signature not found!"
    exit 1
}
