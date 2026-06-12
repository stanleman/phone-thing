param(
    [string]$WatchDir = "app/src/main",
    [int]$PollIntervalMs = 1500,
    [int]$DebounceMs = 2000
)

# PhoneThing Auto-Build Watch Script
# Polls for file changes every 1.5s, rebuilds & reinstalls automatically.
# Simpler than FileSystemWatcher events — no PowerShell scope issues.

$watchPath = Resolve-Path $WatchDir
$extensionFilter = @('.kt', '.xml', '.gradle', '.properties', '.java')
$fileHashes = @{}

# Snapshot current file hashes
function Get-FileSnapshot {
    $snap = @{}
    Get-ChildItem $watchPath -Recurse -File | Where-Object {
        $extensionFilter -contains $_.Extension
    } | ForEach-Object {
        $snap[$_.FullName] = $_.LastWriteTimeUtc.Ticks
    }
    return $snap
}

Write-Host ""
Write-Host "  == PhoneThing Auto-Build Watch ==" -ForegroundColor Cyan
Write-Host "  Watching: $watchPath" -ForegroundColor Gray
Write-Host "  Polling every $($PollIntervalMs)ms" -ForegroundColor Gray
Write-Host "  Press Ctrl+C to stop" -ForegroundColor Gray

$previous = Get-FileSnapshot
$lastBuildTime = 0

while ($true) {
    Start-Sleep -Milliseconds $PollIntervalMs
    $current = Get-FileSnapshot
    $changed = @()

    # Check for new/changed files
    foreach ($kv in $current.GetEnumerator()) {
        if (-not $previous.ContainsKey($kv.Key)) {
            $changed += $kv.Key
        } elseif ($previous[$kv.Key] -ne $kv.Value) {
            $changed += $kv.Key
        }
    }

    # Check for deleted files
    foreach ($kv in $previous.GetEnumerator()) {
        if (-not $current.ContainsKey($kv.Key)) {
            $changed += $kv.Key
        }
    }

    if ($changed.Count -eq 0) {
        $previous = $current
        continue
    }

    $previous = $current

    # Debounce: wait a bit to catch rapid saves, then check again
    Start-Sleep -Milliseconds $DebounceMs
    $current = Get-FileSnapshot
    $stillChanged = $changed | Where-Object {
        $current.ContainsKey($_) -and $previous.ContainsKey($_) -and
        ($current[$_] -ne $previous[$_])
    }

    if ($stillChanged.Count -eq 0 -and $changed.Count -eq 0) { continue }

    $relPaths = ($changed | ForEach-Object {
        $rel = $_.Substring($watchPath.Path.Length + 1)
        if ($rel.Length -gt 60) { $rel.Substring(0, 57) + "..." } else { $rel }
    }) -join ", "

    Write-Host ""
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] File(s) changed: $relPaths" -ForegroundColor Yellow
    $previous = $current

    # Step 1: Build
    Write-Host "  Building APK..." -ForegroundColor Cyan
    $buildOutput = & .\gradlew assembleDebug 2>&1 | Out-String
    $buildOk = $LASTEXITCODE -eq 0

    if ($buildOk) {
        Write-Host "  Build succeeded" -ForegroundColor Green

        # Step 2: Install
        Write-Host "  Installing on device..." -ForegroundColor Cyan
        $installOutput = & adb install -r "app/build/outputs/apk/debug/app-debug.apk" 2>&1 | Out-String
        $installOk = $LASTEXITCODE -eq 0

        if ($installOk) {
            Write-Host "  Install succeeded" -ForegroundColor Green

            # Step 3: Launch
            & adb shell monkey -p com.example.phonething 1 2>&1 | Out-Null
            Write-Host "  Launched!" -ForegroundColor Green
        } else {
            Write-Host "  Install failed:" -ForegroundColor Red
            $installOutput.Trim() | Write-Host
        }
    } else {
        Write-Host "  Build failed" -ForegroundColor Red
        # Show last ~15 lines of build output
        $buildOutput.Trim() -split '\r?\n' | Select-Object -Last 15 | Write-Host
    }

    Write-Host "  ---------------------------------" -ForegroundColor Gray
    $lastBuildTime = [DateTime]::Now.Ticks
}

