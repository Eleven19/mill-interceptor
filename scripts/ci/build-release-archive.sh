#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
target="${2:?target is required}"
archive_name="${3:?archive name is required}"

COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh show modules.mill-interceptor.releaseArchive \
  --version "$version" \
  --target "$target"

test -f "out/modules/mill-interceptor/releaseArchive.dest/$archive_name"
