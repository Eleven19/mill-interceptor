@echo off
setlocal

set "MILLI_LAUNCHER_MODE_VALUE=%MILLI_LAUNCHER_MODE%"
set "MILLI_LAUNCHER_SOURCE_VALUE=%MILLI_LAUNCHER_SOURCE%"
if "%MILLI_LAUNCHER_SOURCE_VALUE%"=="" set "MILLI_LAUNCHER_SOURCE_VALUE=maven"
set "MILLI_LAUNCHER_USE_NETRC_VALUE=%MILLI_LAUNCHER_USE_NETRC%"
if "%MILLI_LAUNCHER_USE_NETRC_VALUE%"=="" set "MILLI_LAUNCHER_USE_NETRC_VALUE=0"
set "MILLI_LAUNCHER_DRY_RUN_VALUE=%MILLI_LAUNCHER_DRY_RUN%"

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

if not exist "%MILLI_CACHE_DIR_VALUE%" mkdir "%MILLI_CACHE_DIR_VALUE%"

set "JAR_NAME=milli-dist-%MILLI_VERSION_VALUE%.jar"
set "JAR_PATH=%MILLI_CACHE_DIR_VALUE%\%JAR_NAME%"
set "MAVEN_ARTIFACT_URL=https://repo1.maven.org/maven2/io/github/eleven19/mill-interceptor/milli-dist/%MILLI_VERSION_VALUE%/%JAR_NAME%"
set "GITHUB_ARTIFACT_URL=https://github.com/Eleven19/mill-interceptor/releases/download/v%MILLI_VERSION_VALUE%/mill-interceptor-dist-v%MILLI_VERSION_VALUE%.jar"

if "%MILLI_LAUNCHER_DRY_RUN_VALUE%"=="1" (
  echo version=%MILLI_VERSION_VALUE%
  echo mode=%MILLI_LAUNCHER_MODE_VALUE%
  echo preferred_source=%MILLI_LAUNCHER_SOURCE_VALUE%
  echo use_netrc=%MILLI_LAUNCHER_USE_NETRC_VALUE%
  echo maven_artifact_url=%MAVEN_ARTIFACT_URL%
  echo github_artifact_url=%GITHUB_ARTIFACT_URL%
  echo jar_path=%JAR_PATH%
  exit /b 0
)

if not exist "%JAR_PATH%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%MAVEN_ARTIFACT_URL%' -OutFile '%JAR_PATH%'"
)

java -jar "%JAR_PATH%" %*
