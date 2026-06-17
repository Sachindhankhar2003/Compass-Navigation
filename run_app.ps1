Write-Host "Waiting for device to connect..."
& "C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe" wait-for-device

Write-Host "Waiting for device state to be online..."
while ($true) {
    $state = & "C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe" get-state 2>$null
    if ($state -eq "device") {
        break
    }
    Write-Host "Current state: $state. Waiting..."
    Start-Sleep -Seconds 2
}

Write-Host "Waiting for Android OS boot to complete..."
while ($true) {
    $booted = & "C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell getprop sys.boot_completed 2>$null
    $booted = $booted.Trim()
    if ($booted -eq "1") {
        break
    }
    Write-Host "Boot status: '$booted'. Waiting..."
    Start-Sleep -Seconds 2
}

Write-Host "Android OS Boot Completed!"
Write-Host "Installing and running the app..."
& "C:\Users\HP\AppData\AndroidCLI\android.exe" run --apks=app/build/outputs/apk/debug/app-debug.apk
Write-Host "App launched successfully!"
