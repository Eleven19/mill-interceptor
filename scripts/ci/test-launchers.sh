#!/usr/bin/env bash
set -euo pipefail

test -f launcher/milli
test -f launcher/milli.bat

rg -F '.mill-interceptor-version' launcher/milli
rg -F '.config/mill-interceptor-version' launcher/milli
rg -F 'MILLI_LAUNCHER_MODE' launcher/milli
rg -F 'MILLI_LAUNCHER_SOURCE' launcher/milli
rg -F 'MILLI_LAUNCHER_USE_NETRC' launcher/milli
rg -F 'MILLI_LAUNCHER_DRY_RUN' launcher/milli
rg -F 'milli-dist' launcher/milli
rg -F 'github.com/Eleven19/mill-interceptor/releases/download' launcher/milli

rg -F '.mill-interceptor-version' launcher/milli.bat
rg -F '.config\mill-interceptor-version' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_MODE' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_SOURCE' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_USE_NETRC' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_DRY_RUN' launcher/milli.bat
rg -F 'milli-dist' launcher/milli.bat
rg -F 'github.com/Eleven19/mill-interceptor/releases/download' launcher/milli.bat
