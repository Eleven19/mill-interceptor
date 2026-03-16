#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
script_path="$repo_root/scripts/ci/recommend-prerelease.sh"

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

git init "$workdir/repo" >/dev/null
cd "$workdir/repo"
git config user.name "Test User"
git config user.email "test@example.com"

echo "base" >README.md
git add README.md
git commit -m "chore: initial release" >/dev/null
git tag v0.1.0

assert_json_contains() {
  local json="$1"
  local expected="$2"
  if ! printf '%s\n' "$json" | rg -F "$expected" >/dev/null; then
    echo "expected output to contain: $expected" >&2
    printf '%s\n' "$json" >&2
    exit 1
  fi
}

git checkout -b feature/prerelease-helper >/dev/null
echo "patch" >>README.md
git add README.md
git commit -m "fix: patch release candidate" >/dev/null

patch_json="$(OUTPUT=json "$script_path")"
assert_json_contains "$patch_json" '"recommended_bump": "patch"'
assert_json_contains "$patch_json" '"recommended_version": "0.1.1-rc.1"'
assert_json_contains "$patch_json" '"branch": "0.1.1-feature-prerelease-helper.1"'

echo "feature" >>README.md
git add README.md
git commit -m "feat: add prerelease helper" >/dev/null

minor_json="$(OUTPUT=json "$script_path")"
assert_json_contains "$minor_json" '"recommended_bump": "minor"'
assert_json_contains "$minor_json" '"recommended_version": "0.2.0-rc.1"'

git tag v0.2.0-rc.1

minor_increment_json="$(OUTPUT=json "$script_path")"
assert_json_contains "$minor_increment_json" '"recommended_version": "0.2.0-rc.2"'

echo "breaking" >>README.md
git add README.md
git commit -m "feat!: break compatibility" >/dev/null

major_json="$(OUTPUT=json "$script_path")"
assert_json_contains "$major_json" '"recommended_bump": "major"'
assert_json_contains "$major_json" '"recommended_version": "1.0.0-rc.1"'

git checkout -b "Feature/Branch-Name" >/dev/null

branch_json="$(OUTPUT=json "$script_path")"
assert_json_contains "$branch_json" '"branch_channel": "feature-branch-name"'
assert_json_contains "$branch_json" '"branch": "1.0.0-feature-branch-name.1"'
