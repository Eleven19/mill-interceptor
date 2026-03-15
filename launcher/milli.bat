@echo off
setlocal enabledelayedexpansion

set "MILLI_LAUNCHER_MODE_VALUE=%MILLI_LAUNCHER_MODE%"
if "%MILLI_LAUNCHER_MODE_VALUE%"=="" set "MILLI_LAUNCHER_MODE_VALUE=auto"
set "MILLI_LAUNCHER_SOURCE_VALUE=%MILLI_LAUNCHER_SOURCE%"
if "%MILLI_LAUNCHER_SOURCE_VALUE%"=="" set "MILLI_LAUNCHER_SOURCE_VALUE=maven"
set "MILLI_LAUNCHER_USE_NETRC_VALUE=%MILLI_LAUNCHER_USE_NETRC%"
if "%MILLI_LAUNCHER_USE_NETRC_VALUE%"=="" set "MILLI_LAUNCHER_USE_NETRC_VALUE=0"
set "MILLI_LAUNCHER_DRY_RUN_VALUE=%MILLI_LAUNCHER_DRY_RUN%"
if "%MILLI_LAUNCHER_DRY_RUN_VALUE%"=="" set "MILLI_LAUNCHER_DRY_RUN_VALUE=0"

if /I not "%MILLI_LAUNCHER_MODE_VALUE%"=="auto" if /I not "%MILLI_LAUNCHER_MODE_VALUE%"=="native" if /I not "%MILLI_LAUNCHER_MODE_VALUE%"=="dist" (
  echo Unsupported MILLI_LAUNCHER_MODE: %MILLI_LAUNCHER_MODE_VALUE% >&2
  exit /b 1
)

if /I not "%MILLI_LAUNCHER_SOURCE_VALUE%"=="maven" if /I not "%MILLI_LAUNCHER_SOURCE_VALUE%"=="github" (
  echo Unsupported MILLI_LAUNCHER_SOURCE: %MILLI_LAUNCHER_SOURCE_VALUE% >&2
  exit /b 1
)

if defined MILLI_VERSION (
  set "MILLI_VERSION_VALUE=%MILLI_VERSION%"
) else if exist ".mill-interceptor-version" (
  set /p MILLI_VERSION_VALUE=<.mill-interceptor-version
) else if exist ".config\mill-interceptor-version" (
  set /p MILLI_VERSION_VALUE=<.config\mill-interceptor-version
) else (
  set "MILLI_VERSION_VALUE=@MILLI_DEFAULT_VERSION@"
)

if defined MILLI_CACHE_DIR (
  set "MILLI_CACHE_DIR_VALUE=%MILLI_CACHE_DIR%"
) else if defined LOCALAPPDATA (
  set "MILLI_CACHE_DIR_VALUE=%LOCALAPPDATA%\milli"
) else (
  set "MILLI_CACHE_DIR_VALUE=%USERPROFILE%\.cache\milli"
)

set "DIST_ARTIFACT=milli-dist"
set "DIST_RELEASE_NAME=mill-interceptor-dist"
set "GITHUB_RELEASE_BASE=https://github.com/Eleven19/mill-interceptor/releases/download"
set "DIST_NAME=%DIST_ARTIFACT%-%MILLI_VERSION_VALUE%.jar"
set "DIST_PATH=%MILLI_CACHE_DIR_VALUE%\%MILLI_VERSION_VALUE%\%DIST_NAME%"
set "DIST_MAVEN_URL=https://repo1.maven.org/maven2/io/github/eleven19/mill-interceptor/%DIST_ARTIFACT%/%MILLI_VERSION_VALUE%/%DIST_NAME%"
set "DIST_GITHUB_URL=%GITHUB_RELEASE_BASE%/v%MILLI_VERSION_VALUE%/%DIST_RELEASE_NAME%-v%MILLI_VERSION_VALUE%.jar"

set "CURL_NETRC_FLAG="
set "WGET_NETRC_FLAG="
if "%MILLI_LAUNCHER_USE_NETRC_VALUE%"=="1" (
  set "CURL_NETRC_FLAG=--netrc"
  set "WGET_NETRC_FLAG=--netrc"
)

set "NATIVE_SUPPORTED=0"
set "NATIVE_ARTIFACT="
set "NATIVE_RELEASE_TARGET="
set "NATIVE_ARCHIVE_NAME="
set "NATIVE_ARCHIVE_PATH="
set "NATIVE_PATH="
set "NATIVE_MAVEN_URL="
set "NATIVE_GITHUB_URL="

if /I "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  set "NATIVE_SUPPORTED=1"
  set "NATIVE_ARTIFACT=milli-native-windows-amd64"
  set "NATIVE_RELEASE_TARGET=x86_64-pc-windows-msvc"
  set "NATIVE_ARCHIVE_NAME=%NATIVE_ARTIFACT%-%MILLI_VERSION_VALUE%.zip"
  set "NATIVE_ARCHIVE_PATH=%MILLI_CACHE_DIR_VALUE%\%MILLI_VERSION_VALUE%\%NATIVE_ARCHIVE_NAME%"
  set "NATIVE_PATH=%MILLI_CACHE_DIR_VALUE%\%MILLI_VERSION_VALUE%\%NATIVE_ARTIFACT%\mill-interceptor.exe"
  set "NATIVE_MAVEN_URL=https://repo1.maven.org/maven2/io/github/eleven19/mill-interceptor/%NATIVE_ARTIFACT%/%MILLI_VERSION_VALUE%/%NATIVE_ARCHIVE_NAME%"
  set "NATIVE_GITHUB_URL=%GITHUB_RELEASE_BASE%/v%MILLI_VERSION_VALUE%/mill-interceptor-v%MILLI_VERSION_VALUE%-x86_64-pc-windows-msvc.zip"
)

if /I "%MILLI_LAUNCHER_SOURCE_VALUE%"=="maven" (
  set "SOURCE_PRIMARY=maven"
  set "SOURCE_SECONDARY=github"
) else (
  set "SOURCE_PRIMARY=github"
  set "SOURCE_SECONDARY=maven"
)

if /I "%MILLI_LAUNCHER_MODE_VALUE%"=="auto" (
  if "%NATIVE_SUPPORTED%"=="1" (
    set "MODE_PRIMARY=native"
    set "MODE_SECONDARY=dist"
    set "MODE_ORDER=native,dist"
  ) else (
    set "MODE_PRIMARY=dist"
    set "MODE_SECONDARY="
    set "MODE_ORDER=dist"
  )
) else if /I "%MILLI_LAUNCHER_MODE_VALUE%"=="native" (
  set "MODE_PRIMARY=native"
  set "MODE_SECONDARY="
  set "MODE_ORDER=native"
) else (
  set "MODE_PRIMARY=dist"
  set "MODE_SECONDARY="
  set "MODE_ORDER=dist"
)

set "SOURCE_ORDER=%SOURCE_PRIMARY%,%SOURCE_SECONDARY%"

if "%MILLI_LAUNCHER_DRY_RUN_VALUE%"=="1" (
  echo version=%MILLI_VERSION_VALUE%
  echo mode=%MILLI_LAUNCHER_MODE_VALUE%
  echo mode_order=%MODE_ORDER%
  echo preferred_source=%MILLI_LAUNCHER_SOURCE_VALUE%
  echo source_order=%SOURCE_ORDER%
  echo use_netrc=%MILLI_LAUNCHER_USE_NETRC_VALUE%
  echo curl_netrc_flag=%CURL_NETRC_FLAG%
  echo native_supported=%NATIVE_SUPPORTED%
  if "%NATIVE_SUPPORTED%"=="1" (
    echo native_artifact=%NATIVE_ARTIFACT%
    echo native_release_target=%NATIVE_RELEASE_TARGET%
    echo native_maven_url=%NATIVE_MAVEN_URL%
    echo native_github_url=%NATIVE_GITHUB_URL%
    echo native_path=%NATIVE_PATH%
  )
  echo dist_artifact=%DIST_ARTIFACT%
  echo dist_maven_url=%DIST_MAVEN_URL%
  echo dist_github_url=%DIST_GITHUB_URL%
  echo dist_path=%DIST_PATH%
  exit /b 0
)

if /I "%MILLI_LAUNCHER_MODE_VALUE%"=="native" if not "%NATIVE_SUPPORTED%"=="1" (
  echo No native milli artifact is available for this platform >&2
  exit /b 1
)

set "RESOLVED_PATH="
set "RESOLVED_KIND="
call :try_mode "%MODE_PRIMARY%"
if not errorlevel 1 goto run_resolved

if not "%MODE_SECONDARY%"=="" (
  call :try_mode "%MODE_SECONDARY%"
  if not errorlevel 1 goto run_resolved
)

if /I "%MILLI_LAUNCHER_MODE_VALUE%"=="native" (
  echo Unable to download a native milli artifact for version %MILLI_VERSION_VALUE% from Maven Central or GitHub Releases >&2
) else (
  echo Unable to download a milli dist artifact for version %MILLI_VERSION_VALUE% from Maven Central or GitHub Releases >&2
)
exit /b 1

:run_resolved
if /I "%RESOLVED_KIND%"=="native" (
  "%RESOLVED_PATH%" %*
) else (
  java -jar "%RESOLVED_PATH%" %*
)
exit /b %ERRORLEVEL%

:try_mode
if /I "%~1"=="native" (
  call :ensure_native "%SOURCE_PRIMARY%"
  if not errorlevel 1 (
    set "RESOLVED_KIND=native"
    set "RESOLVED_PATH=%NATIVE_PATH%"
    exit /b 0
  )
  call :ensure_native "%SOURCE_SECONDARY%"
  if not errorlevel 1 (
    set "RESOLVED_KIND=native"
    set "RESOLVED_PATH=%NATIVE_PATH%"
    exit /b 0
  )
  exit /b 1
)

call :ensure_dist "%SOURCE_PRIMARY%"
if not errorlevel 1 (
  set "RESOLVED_KIND=dist"
  set "RESOLVED_PATH=%DIST_PATH%"
  exit /b 0
)
call :ensure_dist "%SOURCE_SECONDARY%"
if not errorlevel 1 (
  set "RESOLVED_KIND=dist"
  set "RESOLVED_PATH=%DIST_PATH%"
  exit /b 0
)
exit /b 1

:ensure_native
if "%NATIVE_SUPPORTED%"=="0" exit /b 1
if exist "%NATIVE_PATH%" exit /b 0

if /I "%~1"=="maven" (
  set "DOWNLOAD_URL=%NATIVE_MAVEN_URL%"
) else (
  set "DOWNLOAD_URL=%NATIVE_GITHUB_URL%"
)

call :download_file "%DOWNLOAD_URL%" "%NATIVE_ARCHIVE_PATH%"
if errorlevel 1 exit /b 1

if exist "%MILLI_CACHE_DIR_VALUE%\%MILLI_VERSION_VALUE%\%NATIVE_ARTIFACT%" rd /s /q "%MILLI_CACHE_DIR_VALUE%\%MILLI_VERSION_VALUE%\%NATIVE_ARTIFACT%"
mkdir "%MILLI_CACHE_DIR_VALUE%\%MILLI_VERSION_VALUE%\%NATIVE_ARTIFACT%" >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%NATIVE_ARCHIVE_PATH%' '%MILLI_CACHE_DIR_VALUE%\%MILLI_VERSION_VALUE%\%NATIVE_ARTIFACT%'" >nul
if not exist "%NATIVE_PATH%" exit /b 1
exit /b 0

:ensure_dist
if exist "%DIST_PATH%" exit /b 0

if /I "%~1"=="maven" (
  set "DOWNLOAD_URL=%DIST_MAVEN_URL%"
) else (
  set "DOWNLOAD_URL=%DIST_GITHUB_URL%"
)

call :download_file "%DOWNLOAD_URL%" "%DIST_PATH%"
if errorlevel 1 exit /b 1
if exist "%DIST_PATH%" exit /b 0
exit /b 1

:download_file
set "DOWNLOAD_URL=%~1"
set "DOWNLOAD_DEST=%~2"
set "DOWNLOAD_DIR=%~dp2"
set "DOWNLOAD_TEMP=%DOWNLOAD_DEST%.tmp"

if not exist "%DOWNLOAD_DIR%" mkdir "%DOWNLOAD_DIR%" >nul 2>&1

where curl >nul 2>&1
if not errorlevel 1 (
  if "%CURL_NETRC_FLAG%"=="" (
    curl -fsSL "%DOWNLOAD_URL%" -o "%DOWNLOAD_TEMP%" >nul 2>&1
  ) else (
    curl -fsSL %CURL_NETRC_FLAG% "%DOWNLOAD_URL%" -o "%DOWNLOAD_TEMP%" >nul 2>&1
  )
  if errorlevel 1 exit /b 1
  move /y "%DOWNLOAD_TEMP%" "%DOWNLOAD_DEST%" >nul
  exit /b 0
)

where wget >nul 2>&1
if not errorlevel 1 (
  if "%WGET_NETRC_FLAG%"=="" (
    wget -q -O "%DOWNLOAD_TEMP%" "%DOWNLOAD_URL%" >nul 2>&1
  ) else (
    wget -q %WGET_NETRC_FLAG% -O "%DOWNLOAD_TEMP%" "%DOWNLOAD_URL%" >nul 2>&1
  )
  if errorlevel 1 exit /b 1
  move /y "%DOWNLOAD_TEMP%" "%DOWNLOAD_DEST%" >nul
  exit /b 0
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%DOWNLOAD_URL%' -OutFile '%DOWNLOAD_TEMP%'" >nul
if errorlevel 1 exit /b 1
move /y "%DOWNLOAD_TEMP%" "%DOWNLOAD_DEST%" >nul
exit /b 0
