#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
output_file="${GITHUB_OUTPUT:-}"

dist_name="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" ./mill show releaseDistAssetName \
    --version "$version" \
  | sed -n 's/^"//; s/"$//; p' \
  | tail -n 1
)"

if [[ -z "$dist_name" ]]; then
  echo "Unable to determine dist asset name" >&2
  exit 1
fi

if [[ -n "$output_file" ]]; then
  printf 'dist_name=%s\n' "$dist_name" >>"$output_file"
fi

printf '%s\n' "$dist_name"
