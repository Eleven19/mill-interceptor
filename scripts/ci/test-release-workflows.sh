#!/usr/bin/env bash
set -euo pipefail

ci_mill_scripts=(
  scripts/ci/compute-archive-name.sh
  scripts/ci/build-release-archive.sh
  scripts/ci/compute-assembly-name.sh
  scripts/ci/build-release-assembly.sh
  scripts/ci/compute-launcher-name.sh
  scripts/ci/build-release-launcher.sh
  scripts/ci/publish-central.sh
  scripts/ci/test-publish-metadata.sh
)

for script in "${ci_mill_scripts[@]}"; do
  rg -F 'scripts/ci/run-mill.sh' "$script"
  if rg -n '\./mill(\s|$)' "$script" >/dev/null; then
    echo "$script should use scripts/ci/run-mill.sh instead of invoking ./mill directly" >&2
    exit 1
  fi
done

rg -F 'runner: macos-15-intel' .github/workflows/release.yml
rg -F 'runner: macos-15-intel' .github/workflows/publish-central.yml
rg -F 'workflow_dispatch:' .github/workflows/publish-central.yml
rg -F 'version:' .github/workflows/publish-central.yml
rg -F 'description: "Release version (e.g. 1.0.0) - omit the leading v"' .github/workflows/publish-central.yml
rg -F 'required: true' .github/workflows/publish-central.yml
rg -F "version: \${{ inputs.version || '' }}" .github/workflows/publish-central.yml
rg -F 'name: release-extras' .github/workflows/release.yml
rg -F 'path: |' .github/workflows/release.yml
rg -x '            out/releaseAssembly.dest' .github/workflows/release.yml
rg -x '            out/releaseLauncher.dest' .github/workflows/release.yml

rg -F 'secrets.ELEVEN19_SONATYPE_USERNAME' .github/workflows/publish-central.yml
rg -F 'secrets.ELEVEN19_SONATYPE_PASSWORD' .github/workflows/publish-central.yml
rg -F 'secrets.ELEVEN19_IO_PGP_SECRET_BASE64' .github/workflows/publish-central.yml
rg -F 'secrets.ELEVEN19_IO_PGP_PASSPHRASE' .github/workflows/publish-central.yml

if rg -F 'ghaction-import-gpg' .github/workflows/publish-central.yml >/dev/null; then
  echo ".github/workflows/publish-central.yml should not import a GPG key directly" >&2
  exit 1
fi

rg -F 'mill-jvm-version: "graalvm-java25:25.0.1"' build.mill.yaml
test "$(rg -c 'jvmVersion: "graalvm-java25:25.0.1"' build.mill.yaml)" = "2"

test -f scripts/ci/test-namespace-rename.sh
test -f scripts/ci/test-publish-central.sh
test -f scripts/ci/recommend-prerelease.sh
test -f scripts/ci/test-recommend-prerelease.sh
