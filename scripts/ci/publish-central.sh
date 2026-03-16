#!/usr/bin/env bash
set -euo pipefail

module_task="${1:?module task is required}"
version="${2:?version is required}"

sonatype_username="${SONATYPE_USERNAME:?SONATYPE_USERNAME is required}"
sonatype_password="${SONATYPE_PASSWORD:?SONATYPE_PASSWORD is required}"
pgp_passphrase="${PGP_PASSPHRASE:?PGP_PASSPHRASE is required}"
pgp_secret="${PGP_SECRET:?PGP_SECRET is required}"

export MILLI_PUBLISH_VERSION="$version"
export MILL_SONATYPE_USERNAME="$sonatype_username"
export MILL_SONATYPE_PASSWORD="$sonatype_password"
export MILL_PGP_PASSPHRASE="$pgp_passphrase"
export MILL_PGP_SECRET_BASE64="$(printf '%s' "$pgp_secret" | tr -d '\n\r ')"

args=(
  scripts/ci/run-mill.sh
  --no-server
  "$module_task"
)

"${args[@]}"
