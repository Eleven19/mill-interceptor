#!/usr/bin/env bash
set -euo pipefail

version="${1:-1.2.3}"

summary="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" MILLI_PUBLISH_VERSION="$version" \
    ./mill --no-server show publishArtifactSummary
)"

printf '%s\n' "$summary" | rg -F "\"io.github.eleven19.mill-interceptor:milli:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.github.eleven19.mill-interceptor:milli-assembly:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.github.eleven19.mill-interceptor:milli-native-linux-amd64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.github.eleven19.mill-interceptor:milli-native-linux-aarch64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.github.eleven19.mill-interceptor:milli-native-macos-amd64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.github.eleven19.mill-interceptor:milli-native-macos-aarch64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.github.eleven19.mill-interceptor:milli-native-windows-amd64:${version}\""

assembly_payload="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" MILLI_PUBLISH_VERSION="$version" \
    ./mill --no-server show assemblyPublish.publishArtifacts
)"

printf '%s\n' "$assembly_payload" | rg -F "\"id\": \"milli-assembly\""
printf '%s\n' "$assembly_payload" | rg -F "\"milli-assembly-${version}.jar\""

resolved_modules="$(COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" ./mill --no-server resolve _)"

printf '%s\n' "$resolved_modules" | rg -x "assemblyPublish"
printf '%s\n' "$resolved_modules" | rg -x "nativeLinuxAmd64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeLinuxAarch64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeMacosAmd64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeMacosAarch64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeWindowsAmd64Publish"
