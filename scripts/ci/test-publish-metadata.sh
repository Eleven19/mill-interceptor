#!/usr/bin/env bash
set -euo pipefail

version="${1:-1.2.3}"

summary="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" MILLI_PUBLISH_VERSION="$version" \
    scripts/ci/run-mill.sh --no-server show publishArtifactSummary
)"

printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-dist:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-linux-amd64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-linux-aarch64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-macos-amd64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-macos-aarch64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-windows-amd64:${version}\""

dist_payload="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" MILLI_PUBLISH_VERSION="$version" \
    scripts/ci/run-mill.sh --no-server show assemblyPublish.publishArtifacts
)"

printf '%s\n' "$dist_payload" | rg -F "\"id\": \"milli-dist\""
printf '%s\n' "$dist_payload" | rg -F "\"milli-dist-${version}.jar\""

resolved_modules="$(COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh --no-server resolve _)"
resolved_publish_tasks="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh --no-server resolve __.publishSonatypeCentral
)"

printf '%s\n' "$resolved_modules" | rg -x "assemblyPublish"
printf '%s\n' "$resolved_modules" | rg -x "nativeLinuxAmd64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeLinuxAarch64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeMacosAmd64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeMacosAarch64Publish"
printf '%s\n' "$resolved_modules" | rg -x "nativeWindowsAmd64Publish"

printf '%s\n' "$resolved_publish_tasks" | rg -x "publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "assemblyPublish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "nativeLinuxAmd64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "nativeLinuxAarch64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "nativeMacosAmd64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "nativeMacosAarch64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "nativeWindowsAmd64Publish.publishSonatypeCentral"
