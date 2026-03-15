@echo off
setlocal

if defined MILLI_VERSION (
  set "MILLI_VERSION_VALUE=%MILLI_VERSION%"
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

set "JAR_NAME=milli-assembly-%MILLI_VERSION_VALUE%.jar"
set "JAR_PATH=%MILLI_CACHE_DIR_VALUE%\%JAR_NAME%"
set "ARTIFACT_URL=https://repo1.maven.org/maven2/io/github/eleven19/mill-interceptor/milli-assembly/%MILLI_VERSION_VALUE%/%JAR_NAME%"

if not exist "%JAR_PATH%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%ARTIFACT_URL%' -OutFile '%JAR_PATH%'"
)

java -jar "%JAR_PATH%" %*
