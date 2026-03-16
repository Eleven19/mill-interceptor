#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
launcher_os="${2:?launcher os is required}"
launcher_name="${3:?launcher name is required}"

COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh show releaseLauncher \
  --version "$version" \
  --launcher-os "$launcher_os"

test -f "out/releaseLauncher.dest/$launcher_name"
