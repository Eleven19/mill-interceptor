#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
asset_name="${2:?asset name is required}"

COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" ./mill show releaseDist \
  --version "$version"

test -f "out/releaseDist.dest/$asset_name"
