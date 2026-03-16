#!/usr/bin/env bash
set -euo pipefail

artifacts_dir="${1:?artifacts directory is required}"

cd "$artifacts_dir"

release_files=()
while IFS= read -r -d '' path; do
  release_files+=("${path#./}")
done < <(find . -type f ! -name SHA256SUMS -print0 | sort -z)

if [[ "${#release_files[@]}" -eq 0 ]]; then
  echo "No release files found in $artifacts_dir" >&2
  exit 1
fi

sha256sum "${release_files[@]}" > SHA256SUMS
