#!/usr/bin/env bash
set -euo pipefail

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
workspace="$tmpdir/workspace"
staging_dir="$tmpdir/release-extras"

mkdir -p \
  "$workspace/out/modules/mill-interceptor/releaseAssembly.dest" \
  "$workspace/out/modules/mill-interceptor/releaseLauncher.dest"

printf 'assembly jar\n' >"$workspace/out/modules/mill-interceptor/releaseAssembly.dest/mill-interceptor-dist-v0.3.0.jar"
printf 'unix launcher\n' >"$workspace/out/modules/mill-interceptor/releaseLauncher.dest/milli"

bash "$repo_root/scripts/ci/stage-release-extra.sh" \
  "$workspace/out/modules/mill-interceptor/releaseAssembly.dest/mill-interceptor-dist-v0.3.0.jar" \
  "$staging_dir/releaseAssembly.dest"

bash "$repo_root/scripts/ci/stage-release-extra.sh" \
  "$workspace/out/modules/mill-interceptor/releaseLauncher.dest/milli" \
  "$staging_dir/releaseLauncher.dest"

rm -rf "$workspace/out/modules/mill-interceptor/releaseLauncher.dest"
mkdir -p "$workspace/out/modules/mill-interceptor/releaseLauncher.dest"
printf 'windows launcher\n' >"$workspace/out/modules/mill-interceptor/releaseLauncher.dest/milli.bat"

bash "$repo_root/scripts/ci/stage-release-extra.sh" \
  "$workspace/out/modules/mill-interceptor/releaseLauncher.dest/milli.bat" \
  "$staging_dir/releaseLauncher.dest"

test -f "$staging_dir/releaseAssembly.dest/mill-interceptor-dist-v0.3.0.jar"
test -f "$staging_dir/releaseLauncher.dest/milli"
test -f "$staging_dir/releaseLauncher.dest/milli.bat"

rg -F 'assembly jar' "$staging_dir/releaseAssembly.dest/mill-interceptor-dist-v0.3.0.jar"
rg -F 'unix launcher' "$staging_dir/releaseLauncher.dest/milli"
rg -F 'windows launcher' "$staging_dir/releaseLauncher.dest/milli.bat"
