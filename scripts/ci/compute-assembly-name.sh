#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
output_file="${GITHUB_OUTPUT:-}"

assembly_name="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh show releaseAssemblyAssetName \
    --version "$version" \
  | sed -n 's/^"//; s/"$//; p' \
  | tail -n 1
)"

if [[ -z "$assembly_name" ]]; then
  echo "Unable to determine assembly asset name" >&2
  exit 1
fi

if [[ -n "$output_file" ]]; then
  printf 'assembly_name=%s\n' "$assembly_name" >>"$output_file"
fi

printf '%s\n' "$assembly_name"
