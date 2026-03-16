#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
build_file="$repo_root/build.mill.yaml"

resolved_mill_version="$(
  sed -nE 's/^mill-version:[[:space:]]*"?([^"]+)"?[[:space:]]*$/\1/p' "$build_file" | head -n 1
)"

if [[ -z "$resolved_mill_version" ]]; then
  echo "Unable to resolve mill-version from $build_file" >&2
  exit 1
fi

if [[ -z "${MILL_VERSION:-}" ]]; then
  export MILL_VERSION="${resolved_mill_version}-jvm"
fi

exec "$repo_root/mill" "$@"
