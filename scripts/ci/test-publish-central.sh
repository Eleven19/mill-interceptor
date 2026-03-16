#!/usr/bin/env bash
set -euo pipefail

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

mkdir -p "$tmpdir/scripts/ci"
cp -f scripts/ci/publish-central.sh "$tmpdir/scripts/ci/publish-central.sh"

cat >"$tmpdir/scripts/ci/run-mill.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

printf 'MILL_SONATYPE_USERNAME=%s\n' "${MILL_SONATYPE_USERNAME:-}"
printf 'MILL_SONATYPE_PASSWORD=%s\n' "${MILL_SONATYPE_PASSWORD:-}"
printf 'MILL_PGP_PASSPHRASE=%s\n' "${MILL_PGP_PASSPHRASE:-}"
printf 'MILL_PGP_SECRET_BASE64=%s\n' "${MILL_PGP_SECRET_BASE64:-}"
printf 'MILLI_PUBLISH_VERSION=%s\n' "${MILLI_PUBLISH_VERSION:-}"
printf 'ARGS=%s\n' "$*"
EOF

chmod +x "$tmpdir/scripts/ci/publish-central.sh" "$tmpdir/scripts/ci/run-mill.sh"

output="$(
  cd "$tmpdir"
  SONATYPE_USERNAME="user" \
  SONATYPE_PASSWORD="pass" \
  PGP_PASSPHRASE="phrase" \
  PGP_SECRET=$'line-one\nline-two\n' \
  scripts/ci/publish-central.sh assemblyPublish.publish 1.2.3
)"

printf '%s\n' "$output" | rg -x 'MILL_SONATYPE_USERNAME=user'
printf '%s\n' "$output" | rg -x 'MILL_SONATYPE_PASSWORD=pass'
printf '%s\n' "$output" | rg -x 'MILL_PGP_PASSPHRASE=phrase'
printf '%s\n' "$output" | rg -x 'MILL_PGP_SECRET_BASE64=line-oneline-two'
printf '%s\n' "$output" | rg -x 'MILLI_PUBLISH_VERSION=1.2.3'
printf '%s\n' "$output" | rg -x 'ARGS=--no-server assemblyPublish.publish'
