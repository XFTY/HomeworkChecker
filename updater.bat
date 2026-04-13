@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM Check for administrator privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] Requesting administrator privileges...
    
    :retry_admin
    if "%*"=="" (
        powershell -Command "Start-Process '%~f0' -Verb RunAs" 2>nul
    ) else (
        powershell -Command "Start-Process '%~f0' -ArgumentList '%*' -Verb RunAs" 2>nul
    )
    
    REM Check if elevation was successful
    net session >nul 2>&1
    if %errorLevel% neq 0 (
        echo.
        echo [WARNING] Administrator privilege authorization was canceled or failed!
        echo.
        choice /C YN /M "Would you like to retry? Press Y to retry, N to exit"
        if errorlevel 2 (
            echo [INFO] Exiting updater...
            pause
            exit /b 1
        )
        if errorlevel 1 (
            echo.
            echo [INFO] Retrying administrator privilege request...
            goto :retry_admin
        )
    )
)

echo ========================================
echo    HomeworkChecker Updater
echo ========================================
echo.

REM Check parameters
if "%~1"=="" (
    echo [ERROR] No update package path provided!
    echo Usage: updater.bat ^<update_package.zip^> ^<install_directory^>
    pause
    exit /b 1
)

if "%~2"=="" (
    echo [ERROR] No installation directory provided!
    echo Usage: updater.bat ^<update_package.zip^> ^<install_directory^>
    pause
    exit /b 1
)

set UPDATE_PACKAGE=%~1
set INSTALL_DIR=%~2
set TEMP_EXTRACT_DIR=%TEMP%\HomeworkChecker_Update_%RANDOM%

echo [INFO] Update package: %UPDATE_PACKAGE%
echo [INFO] Installation directory: %INSTALL_DIR%
echo.

REM Check if update package exists
if not exist "%UPDATE_PACKAGE%" (
    echo [ERROR] Update package not found: %UPDATE_PACKAGE%
    pause
    exit /b 1
)

REM Check if installation directory exists
if not exist "%INSTALL_DIR%" (
    echo [ERROR] Installation directory not found: %INSTALL_DIR%
    pause
    exit /b 1
)

echo [INFO] Creating temporary extraction directory...
mkdir "%TEMP_EXTRACT_DIR%"
if errorlevel 1 (
    echo [ERROR] Failed to create temporary directory!
    pause
    exit /b 1
)

echo [INFO] Extracting update package...
echo [
echo [0%%                                                  ] 0%%

REM Use PowerShell to extract with progress
powershell -Command ^
    "$zipPath = '%UPDATE_PACKAGE%'; ^
     $extractPath = '%TEMP_EXTRACT_DIR%'; ^
     Add-Type -AssemblyName System.IO.Compression.FileSystem; ^
     $zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath); ^
     $totalFiles = $zip.Entries.Count; ^
     $processedFiles = 0; ^
     foreach ($entry in $zip.Entries) { ^
         if ($entry.FullName -notmatch '/$') { ^
             $destPath = Join-Path $extractPath $entry.FullName; ^
             $destDir = Split-Path $destPath -Parent; ^
             if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }; ^
             [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destPath, $true); ^
             $processedFiles++; ^
             $progress = [math]::Floor(($processedFiles / $totalFiles) * 100); ^
             $barLength = 50; ^
             $filledLength = [math]::Floor(($progress / 100) * $barLength); ^
             $bar = '=' * $filledLength + ' ' * ($barLength - $filledLength); ^
             Write-Host "`r[$progress%% |$bar| $progress%%]" -NoNewline; ^
         } ^
     }; ^
     $zip.Dispose(); ^
     Write-Host ''"

if errorlevel 1 (
    echo.
    echo [ERROR] Failed to extract update package!
    rmdir /s /q "%TEMP_EXTRACT_DIR%"
    pause
    exit /b 1
)

echo.
echo [INFO] Update files extracted successfully!
echo.
echo [INFO] Replacing old files with new version...
echo [

REM Count total files to copy (exclude updater.bat itself)
for /f %%i in ('dir "%TEMP_EXTRACT_DIR%\*.*" /s /b /a-d ^| find /c /v ""') do set TOTAL_FILES=%%i
set COPIED_FILES=0

REM Get the path of this script relative to install directory
set "SCRIPT_PATH=%~f0"
set "UPDATER_IN_INSTALL=false"
echo "!SCRIPT_PATH!" | findstr /I /C:"!INSTALL_DIR!" >nul 2>&1
if not errorlevel 1 set "UPDATER_IN_INSTALL=true"

REM Copy files with progress tracking
for /f "delims=" %%F in ('dir "%TEMP_EXTRACT_DIR%\*.*" /s /b /a-d') do (
    set "REL_PATH=%%F"
    set "REL_PATH=!REL_PATH:%TEMP_EXTRACT_DIR%=!"
    set "DEST_FILE=%INSTALL_DIR%!REL_PATH!"
    
    REM Skip if this is the updater.bat itself and it's in the install directory
    set "SKIP_COPY=false"
    if "!UPDATER_IN_INSTALL!"=="true" (
        if /I "!DEST_FILE!"=="!SCRIPT_PATH!" (
            echo [INFO] Skipping updater.bat to avoid self-deletion
            set "SKIP_COPY=true"
        )
    )
    
    if not "!SKIP_COPY!"=="true" (
        REM Create destination directory if needed
        for %%D in ("!DEST_FILE!") do (
            if not exist "%%~dpD" mkdir "%%~dpD" >nul 2>&1
        )
        
        copy /Y "%%F" "!DEST_FILE!" >nul 2>&1
    )
    
    set /a COPIED_FILES+=1
    REM Calculate and display progress
    set /a PROGRESS=(COPIED_FILES * 100) / TOTAL_FILES
    set /a BAR_LENGTH=PROGRESS / 2
    set "BAR="
    set "SPACE="
    if !BAR_LENGTH! GTR 0 (
        for /L %%B in (1,1,!BAR_LENGTH!) do set "BAR=!BAR!="
    )
    set /a SPACE_COUNT=50-BAR_LENGTH
    if !SPACE_COUNT! GTR 0 (
        for /L %%S in (1,1,!SPACE_COUNT!) do set "SPACE=!SPACE! "
    )
    
    <nul set /p "=[!PROGRESS%% |!BAR!!SPACE!| !PROGRESS%%]"
)

echo.
echo [INFO] Files replaced successfully!
echo.
echo [INFO] Cleaning up temporary files...
rmdir /s /q "%TEMP_EXTRACT_DIR%"

echo [INFO] Update completed successfully!
echo.

REM Ask user if they want to delete the update package
echo [INFO] Update package location: %UPDATE_PACKAGE%
choice /C YN /M "Would you like to delete the update package?"
if errorlevel 2 (
    echo [INFO] Update package kept at: %UPDATE_PACKAGE%
) else (
    echo [INFO] Deleting update package...
    del /F /Q "%UPDATE_PACKAGE%"
    if errorlevel 1 (
        echo [WARNING] Failed to delete update package. You may need to delete it manually.
    ) else (
        echo [INFO] Update package deleted successfully!
    )
)

echo.
echo [INFO] Starting HomeworkChecker...

REM Start the application using VBS launcher
start "" "%INSTALL_DIR%\HomeworkCheckerLauncher.vbs"

echo [INFO] Launcher started. Exiting updater...
timeout /t 2 /nobreak >nul
exit /b 0