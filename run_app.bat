@echo off
echo Waiting for device...
C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe wait-for-device
echo Waiting for Android OS boot to complete...
:loop
for /f "tokens=*" %%a in ('C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe shell getprop sys.boot_completed') do set BOOT_STATUS=%%a
if "%BOOT_STATUS%"=="1" (
    goto booted
)
echo Boot status: %BOOT_STATUS%
:: Safe non-interactive sleep using ping
ping 127.0.0.1 -n 3 > nul
goto loop

:booted
echo Android OS Boot Completed!
echo Installing and running the app...
C:\Users\HP\AppData\AndroidCLI\android.exe run --apks=app/build/outputs/apk/debug/app-debug.apk
echo App launched successfully!
