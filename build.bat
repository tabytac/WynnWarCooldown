@echo off
REM Wynn War Cooldown - Build Script
REM Minecraft 1.21.4 Fabric Mod

echo.
echo ======================================
echo Wynn War Cooldown - Build Script
echo ======================================
echo.

REM Set Java 21 path
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot

REM Check if Java is available
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java 21 not found at %JAVA_HOME%
    echo Please install Eclipse Adoptium JDK 21 or update JAVA_HOME in this script
    pause
    exit /b 1
)

echo Java Home: %JAVA_HOME%
echo Java Version:
"%JAVA_HOME%\bin\java.exe" -version

echo.
echo ======================================
echo Starting Clean Build...
echo ======================================
echo.

cd /d "%~dp0"

REM Run gradle clean build
call gradlew.bat clean build

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================
    echo BUILD SUCCESSFUL!
    echo ======================================
    echo.
    echo JAR Location: build\libs\wynn-war-cooldown-1.0.0.jar
    echo.
    echo Next steps:
    echo 1. Copy the JAR to your mods folder:
    echo    %%APPDATA%%\.minecraft\mods\
    echo.
    echo 2. Ensure these mods are also installed:
    echo    - fabric-api-0.119.4+1.21.4.jar
    echo    - fabric-language-kotlin-1.13.9+kotlin.2.3.10.jar
    echo    - modmenu-13.0.0-beta.3+1.21.4.jar
    echo    - cloth-config-fabric-15.0.140+1.21.4.jar
    echo    - architectury-fabric-13.0.4.jar
    echo.
    echo 3. Launch Minecraft and enjoy!
    echo.
    pause
) else (
    echo.
    echo ======================================
    echo BUILD FAILED!
    echo ======================================
    echo.
    echo Check the error messages above for details.
    echo.
    pause
    exit /b 1
)
