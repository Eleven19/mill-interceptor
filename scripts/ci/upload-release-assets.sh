#!/usr/bin/env bash
set -euo pipefail

release_tag="${RELEASE_TAG:?RELEASE_TAG is required}"
artifacts_dir="${1:?artifacts directory is required}"

release_files=()
while IFS= read -r -d '' path; do
  release_files+=("$path")
done < <(find "$artifacts_dir" -type f -print0 | sort -z)

if [[ "${#release_files[@]}" -eq 0 ]]; then
  echo "No release files found in $artifacts_dir" >&2
  exit 1
fi

gh release upload "$release_tag" "${release_files[@]}" --clobber
