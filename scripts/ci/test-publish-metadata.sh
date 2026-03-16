#!/usr/bin/env bash
set -euo pipefail

version="${1:-1.2.3}"

summary="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" MILLI_PUBLISH_VERSION="$version" \
    scripts/ci/run-mill.sh --no-server show modules.mill-interceptor.publishArtifactSummary
)"
plugin_summary="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" MILLI_PUBLISH_VERSION="$version" \
    scripts/ci/run-mill.sh --no-server show modules.mill-interceptor-maven-plugin.publishArtifactSummary
)"

printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-dist:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-linux-amd64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-linux-aarch64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-macos-amd64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-macos-aarch64:${version}\""
printf '%s\n' "$summary" | rg -F "\"io.eleven19.mill-interceptor:milli-native-windows-amd64:${version}\""
printf '%s\n' "$plugin_summary" | rg -F "\"io.eleven19.mill-interceptor:mill-interceptor-maven-plugin:${version}\""

dist_payload="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" MILLI_PUBLISH_VERSION="$version" \
    scripts/ci/run-mill.sh --no-server show modules.mill-interceptor.assemblyPublish.publishArtifacts
)"

printf '%s\n' "$dist_payload" | rg -F "\"id\": \"milli-dist\""
printf '%s\n' "$dist_payload" | rg -F "\"milli-dist-${version}.jar\""

resolved_modules="$(COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh --no-server resolve __)"
resolved_publish_tasks="$(
  COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" scripts/ci/run-mill.sh --no-server resolve __.publishSonatypeCentral
)"

printf '%s\n' "$resolved_modules" | rg -x "modules"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor.assemblyPublish"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor.nativeLinuxAmd64Publish"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor.nativeLinuxAarch64Publish"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor.nativeMacosAmd64Publish"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor.nativeMacosAarch64Publish"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor.nativeWindowsAmd64Publish"
printf '%s\n' "$resolved_modules" | rg -x "modules.mill-interceptor-maven-plugin"

printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor.assemblyPublish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor.nativeLinuxAmd64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor.nativeLinuxAarch64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor.nativeMacosAmd64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor.nativeMacosAarch64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor.nativeWindowsAmd64Publish.publishSonatypeCentral"
printf '%s\n' "$resolved_publish_tasks" | rg -x "modules.mill-interceptor-maven-plugin.publishSonatypeCentral"
