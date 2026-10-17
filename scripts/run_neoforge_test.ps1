param(
    [string]$Server = "example-server.com",
    [string]$McVersion = "",
    [int]$TimeoutSeconds = 40
)

$projectRoot = (Resolve-Path "$PSScriptRoot/..").Path

if (-not $McVersion) {
    $propsPath = "$projectRoot/gradle.properties"
    if (Test-Path $propsPath) {
        $line = Get-Content $propsPath | Select-String "minecraft_version=" | Select-Object -First 1
        if ($line) {
            $McVersion = $line.ToString().Split('=')[1].Trim()
        }
    }
    if (-not $McVersion) {
        $McVersion = "1.21"
    }
}

# Determine ATLauncher / instance directory dynamically across any machine
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

$instanceName = "Minecraft$($McVersion.Replace('.', ''))withNeoForge"
$instanceDir = Join-Path $usersDir "instances/$instanceName"

# If exact named instance doesn't exist, search for any instance matching NeoForge and version
if (-not (Test-Path $instanceDir)) {
    $instancesRoot = Join-Path $usersDir "instances"
    if (Test-Path $instancesRoot) {
        $found = Get-ChildItem $instancesRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match "NeoForge" -and $_.Name -match [regex]::Escape($McVersion.Replace('.', '')) } |
            Select-Object -First 1
        if ($found) {
            $instanceDir = $found.FullName
            $instanceName = $found.Name
        }
    }
}

if (-not (Test-Path $instanceDir)) {
    Write-Error "NeoForge instance for Minecraft $McVersion was not found at '$instanceDir'. Please ensure an instance named '$instanceName' exists in ATLauncher, or set the ATLAUNCHER_DIR environment variable."
    exit 1
}

$instanceModsDir = Join-Path $instanceDir "mods"
$testModsNeo = Join-Path $projectRoot "test_mods/neoforge"

# Automatically copy any custom test mods provided in test_mods/neoforge
if (Test-Path $testModsNeo) {
    $customMods = Get-ChildItem $testModsNeo -Filter "*.jar" -ErrorAction SilentlyContinue
    if ($customMods.Count -gt 0) {
        if (-not (Test-Path $instanceModsDir)) {
            New-Item -ItemType Directory -Force -Path $instanceModsDir | Out-Null
        }
        foreach ($m in $customMods) {
            Copy-Item $m.FullName -Destination $instanceModsDir -Force
        }
        Write-Output "==> Loaded $($customMods.Count) custom mod(s) from test_mods/neoforge into NeoForge instance mods folder"
    }
}

$logPath = Join-Path $instanceDir "logs/latest.log"

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
    $p = $p.Replace("USERSDIR\instances\Minecraft121withNeoForge", "USERSDIR\instances\$instanceName")
    $p = $p.Replace("USERSDIR", $usersDir)
    if ($p -eq "1.21") {
        $p = $McVersion
    }
    $resolvedArgs += $p
}

$javaExe = $resolvedArgs[0]
if (-not $javaExe.EndsWith(".exe") -and $isWin) {
    $javaExe = "$javaExe.exe"
}
$javaExe = $javaExe.Replace("javaw.exe", "java.exe")

# Fallback to system or JAVA_HOME java if bundled ATLauncher java is not present on this machine
if (-not (Test-Path $javaExe)) {
    if ($env:JAVA_HOME) {
        $candidate = if ($isWin) { Join-Path $env:JAVA_HOME "bin/java.exe" } else { Join-Path $env:JAVA_HOME "bin/java" }
        if (Test-Path $candidate) {
            $javaExe = $candidate
        }
    }
    if (-not (Test-Path $javaExe)) {
        $javaExe = "java"
    }
}

$remainingArgs = $resolvedArgs[1..($resolvedArgs.Count - 1)]

# QuickPlay parameters
if ($Server) {
    $remainingArgs += "--quickPlayMultiplayer"
    $remainingArgs += $Server
}

Write-Output "==> Starting NeoForge ($instanceName) targeting '$Server'..."

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaExe
foreach ($a in $remainingArgs) {
    $psi.ArgumentList.Add($a)
}
$psi.WorkingDirectory = $instanceDir
$psi.UseShellExecute = $false
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
