#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

node_version="$(node --version | sed 's/^v//' | cut -d. -f1)"
if [[ "$node_version" -lt 22 ]]; then
  echo "Node.js 22+ is required, found v${node_version}" >&2
  exit 1
fi

echo "Running unit tests..."
node --test "$repo_root/launcher/milli.test.mjs"

echo "Running dry-run integration tests..."

assert_contains() {
  local haystack="$1"
  local needle="$2"
  if ! printf '%s\n' "$haystack" | grep -F "$needle" >/dev/null; then
    printf 'FAIL: expected output to contain: %s\n' "$needle" >&2
    printf 'Output was:\n%s\n' "$haystack" >&2
    exit 1
  fi
}

platform_key="$(uname -s):$(uname -m)"

case "$platform_key" in
  Linux:x86_64)
    expected_native_artifact='milli-native-linux-amd64'
    ;;
  Linux:aarch64 | Linux:arm64)
    expected_native_artifact='milli-native-linux-aarch64'
    ;;
  Darwin:x86_64)
    expected_native_artifact='milli-native-macos-amd64'
    ;;
  Darwin:arm64)
    expected_native_artifact='milli-native-macos-aarch64'
    ;;
  *)
    expected_native_artifact=''
    ;;
esac

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

env_output="$(
  cd "$tmpdir"
  printf '%s\n' '9.9.9' >.mill-interceptor-version
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$env_output" 'version=1.2.3'

file_output="$(
  cd "$tmpdir"
  rm -f .config/mill-interceptor-version
  printf '%s\n' '2.0.0' >.mill-interceptor-version
  MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$file_output" 'version=2.0.0'

config_output="$(
  cd "$tmpdir"
  rm -f .mill-interceptor-version
  mkdir -p .config
  printf '%s\n' '3.0.0' >.config/mill-interceptor-version
  MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$config_output" 'version=3.0.0'

default_output="$(
  cd "$tmpdir"
  rm -f .mill-interceptor-version .config/mill-interceptor-version
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$default_output" 'mode=auto'
assert_contains "$default_output" 'source_order=maven,github'
assert_contains "$default_output" 'dist_artifact=milli-dist'

if [[ -n "$expected_native_artifact" ]]; then
  assert_contains "$default_output" 'mode_order=native,dist'
  assert_contains "$default_output" "native_artifact=${expected_native_artifact}"
fi

native_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_MODE='native' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$native_output" 'mode=native'
assert_contains "$native_output" 'mode_order=native'

dist_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_MODE='dist' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$dist_output" 'mode=dist'
assert_contains "$dist_output" 'mode_order=dist'

netrc_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_USE_NETRC=1 MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$netrc_output" 'use_netrc=1'
assert_contains "$netrc_output" 'curl_netrc_flag=--netrc'

echo "All milli.mjs tests passed."
