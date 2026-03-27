#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
script_path="$repo_root/scripts/ci/dispatch-release-suite.sh"

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

cat >"$tmpdir/gh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >>"$GH_LOG"
EOF
chmod +x "$tmpdir/gh"

gh_log="$tmpdir/gh.log"
: >"$gh_log"

(
  cd "$repo_root"
  GH_BIN="$tmpdir/gh" GH_LOG="$gh_log" "$script_path" --ref main --version 1.2.3-rc.1
)

rg -Fx 'workflow run release.yml --ref main -f version=1.2.3-rc.1' "$gh_log"
rg -Fx 'workflow run publish-central.yml --ref main -f version=1.2.3-rc.1' "$gh_log"

: >"$gh_log"

(
  cd "$repo_root"
  GH_BIN="$tmpdir/gh" GH_LOG="$gh_log" "$script_path" --ref main --version 1.2.3-rc.1 --github-only
)

rg -Fx 'workflow run release.yml --ref main -f version=1.2.3-rc.1' "$gh_log"
if rg -F 'publish-central.yml' "$gh_log" >/dev/null; then
  echo "--github-only should not dispatch publish-central.yml" >&2
  exit 1
fi

: >"$gh_log"

(
  cd "$repo_root"
  GH_BIN="$tmpdir/gh" GH_LOG="$gh_log" "$script_path" --ref main --version 1.2.3-rc.1 --maven-central-only
)

rg -Fx 'workflow run publish-central.yml --ref main -f version=1.2.3-rc.1' "$gh_log"
if rg -F 'release.yml' "$gh_log" >/dev/null; then
  echo "--maven-central-only should not dispatch release.yml" >&2
  exit 1
fi
