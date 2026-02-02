@echo off
echo ========================================
echo   Rubidium Framework UI Launcher
echo   Version 1.0
echo ========================================
echo.

set JAR_PATH=build\libs\rubidium-dev-1.0.jar

if not exist "%JAR_PATH%" (
    echo ERROR: JAR not found at %JAR_PATH%
    echo Please run: gradlew rubidiumDevJar
    pause
    exit /b 1
)

echo Starting Rubidium UI...
java -cp "%JAR_PATH%;build\libs\*" rubidium.RubidiumLauncher

pause
