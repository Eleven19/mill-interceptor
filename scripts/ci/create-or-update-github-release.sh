#!/usr/bin/env bash
set -euo pipefail

release_tag="${RELEASE_TAG:?RELEASE_TAG is required}"
release_name="${RELEASE_NAME:?RELEASE_NAME is required}"
release_prerelease="${RELEASE_PRERELEASE:?RELEASE_PRERELEASE is required}"
repo="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"

if gh release view "$release_tag" >/dev/null 2>&1; then
  release_id="$(gh release view "$release_tag" --json databaseId -q .databaseId)"

  gh api \
    --method PATCH \
    "repos/$repo/releases/$release_id" \
    --field tag_name="$release_tag" \
    --field name="$release_name" \
    --field prerelease="$release_prerelease"
else
  gh api \
    --method POST \
    "repos/$repo/releases" \
    --field tag_name="$release_tag" \
    --field name="$release_name" \
    --field prerelease="$release_prerelease" \
    --field generate_release_notes=true
fi
