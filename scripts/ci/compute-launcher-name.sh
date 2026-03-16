#!/usr/bin/env bash
set -euo pipefail

launcher_os="${1:?launcher os is required}"
output_name="${2:?output name is required}"
output_file="${GITHUB_OUTPUT:-}"

launcher_name="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh show releaseLauncherName \
    --launcher-os "$launcher_os" \
  | sed -n 's/^"//; s/"$//; p' \
  | tail -n 1
)"

if [[ -z "$launcher_name" ]]; then
  echo "Unable to determine launcher name for $launcher_os" >&2
  exit 1
fi

if [[ -n "$output_file" ]]; then
  printf '%s=%s\n' "$output_name" "$launcher_name" >>"$output_file"
fi

printf '%s\n' "$launcher_name"
