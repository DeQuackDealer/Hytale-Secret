@echo off
echo Starting Rubidium Test Server Launcher...

cd /d "%~dp0"

if not exist "launcher\build\libs\TestServerLauncher.jar" (
    echo Building launcher first...
    cd launcher
    call gradlew.bat fatJar --quiet
    cd ..
)

java -jar launcher\build\libs\TestServerLauncher.jar
pause
