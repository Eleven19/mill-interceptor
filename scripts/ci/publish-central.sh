#!/usr/bin/env bash
set -euo pipefail

module_task="${1:?module task is required}"
version="${2:?version is required}"

sonatype_username="${SONATYPE_USERNAME:?SONATYPE_USERNAME is required}"
sonatype_password="${SONATYPE_PASSWORD:?SONATYPE_PASSWORD is required}"
gpg_passphrase="${SONATYPE_GPG_PASSPHRASE:?SONATYPE_GPG_PASSPHRASE is required}"

export MILLI_PUBLISH_VERSION="$version"

args=(
  scripts/ci/run-mill.sh
  --no-server
  "$module_task"
  --sonatypeCreds
  "${sonatype_username}:${sonatype_password}"
  --signed
  true
  --release
  true
  --gpgArgs
  --batch
  --gpgArgs
  --yes
  --gpgArgs
  --pinentry-mode
  --gpgArgs
  loopback
  --gpgArgs
  --passphrase
  --gpgArgs
  "$gpg_passphrase"
)

"${args[@]}"
