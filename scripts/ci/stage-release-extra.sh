#!/usr/bin/env bash
set -euo pipefail

source_path="${1:?source path is required}"
destination_dir="${2:?destination directory is required}"

if [[ ! -f "$source_path" ]]; then
  echo "Source file not found: $source_path" >&2
  exit 1
fi

mkdir -p "$destination_dir"
cp -f "$source_path" "$destination_dir/"
