#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
asset_name="${2:?asset name is required}"

COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh show modules.mill-interceptor.releaseAssembly \
  --version "$version"

test -f "out/modules/mill-interceptor/releaseAssembly.dest/$asset_name"
