#!/usr/bin/env bash
set -euo pipefail

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

artifacts_dir="$tmpdir/release-artifacts"
mkdir -p "$artifacts_dir/releaseAssembly.dest" "$artifacts_dir/releaseLauncher.dest" "$tmpdir/bin"

printf 'linux archive\n' >"$artifacts_dir/mill-interceptor-v0.1.0-x86_64-unknown-linux-gnu.tar.gz"
printf 'assembly jar\n' >"$artifacts_dir/releaseAssembly.dest/mill-interceptor-dist-v0.1.0.jar"
printf 'unix launcher\n' >"$artifacts_dir/releaseLauncher.dest/milli"
printf 'windows launcher\n' >"$artifacts_dir/releaseLauncher.dest/milli.bat"

scripts/ci/generate-checksums.sh "$artifacts_dir"
test -f "$artifacts_dir/SHA256SUMS"
rg -F 'mill-interceptor-v0.1.0-x86_64-unknown-linux-gnu.tar.gz' "$artifacts_dir/SHA256SUMS"
rg -F 'releaseAssembly.dest/mill-interceptor-dist-v0.1.0.jar' "$artifacts_dir/SHA256SUMS"
rg -F 'releaseLauncher.dest/milli' "$artifacts_dir/SHA256SUMS"
rg -F 'releaseLauncher.dest/milli.bat' "$artifacts_dir/SHA256SUMS"

cat >"$tmpdir/bin/gh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$@" >"$GH_ARGS_FILE"
EOF
chmod +x "$tmpdir/bin/gh"

export PATH="$tmpdir/bin:$PATH"
export GH_ARGS_FILE="$tmpdir/gh-args.txt"
export RELEASE_TAG="v0.1.0"

scripts/ci/upload-release-assets.sh "$artifacts_dir"

test -f "$GH_ARGS_FILE"
rg -F 'release' "$GH_ARGS_FILE"
rg -F 'upload' "$GH_ARGS_FILE"
rg -F 'v0.1.0' "$GH_ARGS_FILE"
rg -F "$artifacts_dir/mill-interceptor-v0.1.0-x86_64-unknown-linux-gnu.tar.gz" "$GH_ARGS_FILE"
rg -F "$artifacts_dir/releaseAssembly.dest/mill-interceptor-dist-v0.1.0.jar" "$GH_ARGS_FILE"
rg -F "$artifacts_dir/releaseLauncher.dest/milli" "$GH_ARGS_FILE"
rg -F "$artifacts_dir/releaseLauncher.dest/milli.bat" "$GH_ARGS_FILE"
rg -F "$artifacts_dir/SHA256SUMS" "$GH_ARGS_FILE"
