#!/usr/bin/env bash
set -euo pipefail

repo_root="$(pwd)"
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

assert_contains() {
  local haystack="$1"
  local needle="$2"
  printf '%s\n' "$haystack" | rg -F "$needle" >/dev/null
}

test -f launcher/milli
test -f launcher/milli.bat

rg -F '.mill-interceptor-version' launcher/milli
rg -F '.config/mill-interceptor-version' launcher/milli
rg -F 'MILLI_LAUNCHER_MODE' launcher/milli
rg -F 'MILLI_LAUNCHER_SOURCE' launcher/milli
rg -F 'MILLI_LAUNCHER_USE_NETRC' launcher/milli
rg -F 'MILLI_LAUNCHER_DRY_RUN' launcher/milli
rg -F 'milli-dist' launcher/milli
rg -F 'github.com/Eleven19/mill-interceptor/releases/download' launcher/milli

rg -F '.mill-interceptor-version' launcher/milli.bat
rg -F '.config\mill-interceptor-version' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_MODE' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_SOURCE' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_USE_NETRC' launcher/milli.bat
rg -F 'MILLI_LAUNCHER_DRY_RUN' launcher/milli.bat
rg -F 'milli-dist' launcher/milli.bat
rg -F 'github.com/Eleven19/mill-interceptor/releases/download' launcher/milli.bat

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

env_output="$(
  cd "$tmpdir"
  printf '%s\n' '9.9.9' >.mill-interceptor-version
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_DRY_RUN=1 "$repo_root/launcher/milli"
)"

assert_contains "$env_output" 'version=1.2.3'

file_output="$(
  cd "$tmpdir"
  rm -f .config/mill-interceptor-version
  printf '%s\n' '2.0.0' >.mill-interceptor-version
  MILLI_LAUNCHER_DRY_RUN=1 "$repo_root/launcher/milli"
)"

assert_contains "$file_output" 'version=2.0.0'

config_output="$(
  cd "$tmpdir"
  rm -f .mill-interceptor-version
  mkdir -p .config
  printf '%s\n' '3.0.0' >.config/mill-interceptor-version
  MILLI_LAUNCHER_DRY_RUN=1 "$repo_root/launcher/milli"
)"

assert_contains "$config_output" 'version=3.0.0'

default_output="$(
  cd "$tmpdir"
  rm -f .mill-interceptor-version .config/mill-interceptor-version
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_DRY_RUN=1 "$repo_root/launcher/milli"
)"

assert_contains "$default_output" 'mode=auto'
assert_contains "$default_output" 'mode_order=native,dist'
assert_contains "$default_output" 'source_order=maven,github'
assert_contains "$default_output" "native_artifact=${expected_native_artifact}"
assert_contains "$default_output" 'dist_artifact=milli-dist'

native_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_MODE='native' MILLI_LAUNCHER_DRY_RUN=1 "$repo_root/launcher/milli"
)"

assert_contains "$native_output" 'mode=native'
assert_contains "$native_output" 'mode_order=native'

dist_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_MODE='dist' MILLI_LAUNCHER_DRY_RUN=1 "$repo_root/launcher/milli"
)"

assert_contains "$dist_output" 'mode=dist'
assert_contains "$dist_output" 'mode_order=dist'

netrc_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_USE_NETRC=1 MILLI_LAUNCHER_DRY_RUN=1 "$repo_root/launcher/milli"
)"

assert_contains "$netrc_output" 'use_netrc=1'
assert_contains "$netrc_output" 'curl_netrc_flag=--netrc'
