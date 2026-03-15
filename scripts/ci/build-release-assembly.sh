#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
asset_name="${2:?asset name is required}"

COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" ./mill show releaseAssembly \
  --version "$version"

test -f "out/releaseAssembly.dest/$asset_name"
