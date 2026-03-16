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

rg -F 'secrets.SONATYPE_USERNAME || secrets.CENTRAL_PORTAL_USERNAME' .github/workflows/publish-central.yml
rg -F 'secrets.SONATYPE_PASSWORD || secrets.CENTRAL_PORTAL_PASSWORD' .github/workflows/publish-central.yml
rg -F 'secrets.SONATYPE_GPG_PRIVATE_KEY || secrets.CENTRAL_SIGNING_KEY' .github/workflows/publish-central.yml
rg -F 'secrets.SONATYPE_GPG_PASSPHRASE || secrets.CENTRAL_SIGNING_PASSWORD' .github/workflows/publish-central.yml
