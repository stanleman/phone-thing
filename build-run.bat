@echo off
call ./gradlew assembleDebug
if %errorlevel% neq 0 (
    echo Build failed!
    exit /b %errorlevel%
)
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.example.phonething 1
echo Done.
