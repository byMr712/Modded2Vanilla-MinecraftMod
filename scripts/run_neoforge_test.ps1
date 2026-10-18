param(
    [string]$Server = "example-server.com",
    [string]$McVersion = "",
    [int]$TimeoutSeconds = 40,
    [switch]$SkipModCopy,
    [switch]$WaitForJoin
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

if (-not $SkipModCopy) {
    # Clean stale third-party mods, keeping MrModded2Vanilla
    if (Test-Path $instanceModsDir) {
        Get-ChildItem "$instanceModsDir/*.jar" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch "MrModded2Vanilla" } |
            Remove-Item -Force -ErrorAction SilentlyContinue
    } else {
        New-Item -ItemType Directory -Force -Path $instanceModsDir | Out-Null
    }

    # Automatically copy custom test mods provided in test_mods/<version>/neoforge or test_mods/neoforge
    $modsSourceNeo = if (Test-Path "$projectRoot/test_mods/$McVersion/neoforge") {
        "$projectRoot/test_mods/$McVersion/neoforge"
    } elseif (Test-Path $testModsNeo) {
        $testModsNeo
    } else {
        $null
    }

    if ($modsSourceNeo) {
        $customMods = Get-ChildItem $modsSourceNeo -Filter "*.jar" -ErrorAction SilentlyContinue
        if ($customMods.Count -gt 0) {
            foreach ($m in $customMods) {
                Copy-Item $m.FullName -Destination $instanceModsDir -Force
            }
            Write-Output "==> Loaded $($customMods.Count) custom mod(s) from $modsSourceNeo into NeoForge instance mods folder"
        }
    }
}

$logPath = Join-Path $instanceDir "logs/latest.log"

if (Test-Path $logPath) {
    Remove-Item $logPath -Force -ErrorAction SilentlyContinue
}

$jsonPath = Join-Path $instanceDir "instance.json"
$resolvedArgs = @()
$javaExe = ""

if (Test-Path $jsonPath) {
    $json = Get-Content $jsonPath -Raw | ConvertFrom-Json
    $mcVer = if ($McVersion) { $McVersion } else { $json.id }
    $sep = if ($isWin) { ";" } else { ":" }

    $libMap = [ordered]@{}
    foreach ($lib in $json.libraries) {
        if ($lib.name) {
            if ($lib.rules) {
                $allow = $true
                foreach ($rule in $lib.rules) {
                    if ($rule.action -eq "allow" -and $rule.os) {
                        if ($rule.os.name -eq "osx" -and $isWin) { $allow = $false }
                        if ($rule.os.name -eq "linux" -and $isWin) { $allow = $false }
                        if ($rule.os.name -eq "windows" -and -not $isWin) { $allow = $false }
                    }
                    if ($rule.action -eq "disallow" -and $rule.os) {
                        if ($rule.os.name -eq "windows" -and $isWin) { $allow = $false }
                    }
                }
                if (-not $allow) { continue }
            }
            $parts = $lib.name.Split(':')
            if ($parts.Count -ge 2) {
                $classifier = if ($parts.Count -ge 4) { $parts[3] } else { "" }
                $key = "$($parts[0]):$($parts[1]):$classifier"
                $libMap[$key] = $lib
            }
        }
    }

    # Discover modular jars passed via -p / --module-path
    $modulePathJars = @()
    $hasP = $false
    foreach ($item in $json.arguments.jvm) {
        if ($item -is [string]) {
            if ($hasP) {
                $hasP = $false
                $parts = ($item -split '\$\{classpath_separator\}|[;:]') | Where-Object { $_ -match '\.jar$' }
                $modulePathJars += $parts
            } elseif ($item -eq "-p" -or $item -eq "--module-path") {
                $hasP = $true
            }
        }
    }

    $cpList = @()
    foreach ($lib in $libMap.Values) {
        if ($lib.downloads.artifact.path) {
            $artifactLeaf = Split-Path $lib.downloads.artifact.path -Leaf
            $isModuleJar = $false
            foreach ($mj in $modulePathJars) {
                $mjLeaf = Split-Path $mj -Leaf
                $mjBase = $mjLeaf -replace '-\d.*\.jar$', ''
                $artBase = $artifactLeaf -replace '-\d.*\.jar$', ''
                if ($mjBase -and $artBase -and ($mjBase -eq $artBase)) {
                    $isModuleJar = $true
                    break
                }
            }
            if ($isModuleJar) {
                continue
            }
            $p = Join-Path "$usersDir\libraries" $lib.downloads.artifact.path
            if (Test-Path $p) {
                $cpList += $p
            }
        }
    }
    $clientJar = Join-Path "$usersDir\libraries" "net/minecraft/client/$mcVer/client-$mcVer.jar"
    if (Test-Path $clientJar) { $cpList += $clientJar }
    $cpList = $cpList | Select-Object -Unique
    $classpath = $cpList -join $sep

    $nativesDir = Join-Path $usersDir "temp/natives"
    if (-not (Test-Path $nativesDir)) { New-Item -ItemType Directory -Force -Path $nativesDir | Out-Null }

    $jvmArgs = @("-Xmx4096M")
    foreach ($item in $json.arguments.jvm) {
        if ($item -is [string]) {
            $val = $item.Replace('${natives_directory}', $nativesDir)
            $val = $val.Replace('${launcher_name}', 'ATLauncher')
            $val = $val.Replace('${launcher_version}', '3.4.41.3')
            $val = $val.Replace('${classpath}', $classpath)
            $val = $val.Replace('${version_name}', $mcVer)
            $val = $val.Replace('${library_directory}', "$usersDir\libraries")
            $val = $val.Replace('${classpath_separator}', $sep)
            $jvmArgs += $val
        }
    }

    $jvmArgs += $json.mainClass

    $gameArgs = @()
    foreach ($item in $json.arguments.game) {
        if ($item -is [string]) {
            $val = $item.Replace('${auth_player_name}', 'Player')
            $val = $val.Replace('${version_name}', $mcVer)
            $val = $val.Replace('${game_directory}', $instanceDir)
            $val = $val.Replace('${assets_root}', "$usersDir\assets")
            $val = $val.Replace('${assets_index_name}', $json.assetIndex.id)
            $val = $val.Replace('${auth_uuid}', '00000000-0000-0000-0000-000000000000')
            $val = $val.Replace('${auth_access_token}', '0')
            $val = $val.Replace('${clientid}', '0')
            $val = $val.Replace('${auth_xuid}', '0')
            $val = $val.Replace('${user_type}', 'msa')
            $val = $val.Replace('${version_type}', 'release')
            $gameArgs += $val
        }
    }

    if ($Server) {
        $gameArgs += @("--quickPlayMultiplayer", $Server)
    }

    $resolvedArgs = $jvmArgs + $gameArgs
    $comp = if ($json.javaVersion.component) { $json.javaVersion.component } else { "java-runtime-delta" }
    $bundledJava = Join-Path $usersDir "runtimes/minecraft/$comp/windows-x64/$comp/bin/java.exe"
    if (Test-Path $bundledJava) {
        $javaExe = $bundledJava
    } elseif ($env:JAVA_HOME) {
        $candidate = if ($isWin) { Join-Path $env:JAVA_HOME "bin/java.exe" } else { Join-Path $env:JAVA_HOME "bin/java" }
        if (Test-Path $candidate) { $javaExe = $candidate }
    }
    if (-not $javaExe) { $javaExe = "java" }
    $remainingArgs = $resolvedArgs
} else {
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

    if ($Server) {
        $remainingArgs += "--quickPlayMultiplayer"
        $remainingArgs += $Server
    }
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
        $targetSig = if ($WaitForJoin) {
            "Connecting to $($Server.Replace(':', ', '))|Connecting to $Server|Connected to the server|Joined the game|Loaded \d+ recipes"
        } else {
            "Connecting to $Server|Captured pure vanilla block state snapshot"
        }
        if ($rawLog -match $targetSig) {
            $success = $true
            Write-Output "==> Success signature detected: $($Matches[0])"
            Start-Sleep -Seconds 5
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
