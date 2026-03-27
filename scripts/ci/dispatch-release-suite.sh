#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/ci/dispatch-release-suite.sh --version <version> [--ref <git-ref>]
       scripts/ci/dispatch-release-suite.sh --version <version> [--ref <git-ref>] [--github-only | --maven-central-only]

Dispatch both release workflows with the same version/ref so GitHub Releases
and Maven Central publication start together in parallel.

Examples:
  scripts/ci/dispatch-release-suite.sh --version 1.2.3-rc.1
  scripts/ci/dispatch-release-suite.sh --ref main --version 1.2.3
  scripts/ci/dispatch-release-suite.sh --ref main --version 1.2.3-rc.1 --maven-central-only
EOF
}

version=""
ref=""
dispatch_release=true
dispatch_central=true

while (($# > 0)); do
  case "$1" in
    --version)
      version="${2:-}"
      shift 2
      ;;
    --ref)
      ref="${2:-}"
      shift 2
      ;;
    --github-only)
      dispatch_release=true
      dispatch_central=false
      shift
      ;;
    --maven-central-only)
      dispatch_release=false
      dispatch_central=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$version" ]]; then
  echo "--version is required" >&2
  usage >&2
  exit 1
fi

if [[ "$dispatch_release" != true && "$dispatch_central" != true ]]; then
  echo "At least one workflow must be selected" >&2
  exit 1
fi

if [[ -z "$ref" ]]; then
  ref="$(git rev-parse --abbrev-ref HEAD)"
  if [[ "$ref" == "HEAD" ]]; then
    echo "Unable to infer branch ref from detached HEAD; pass --ref explicitly" >&2
    exit 1
  fi
fi

gh_bin="${GH_BIN:-gh}"

release_cmd=("$gh_bin" workflow run release.yml --ref "$ref" -f "version=$version")
central_cmd=("$gh_bin" workflow run publish-central.yml --ref "$ref" -f "version=$version")

if [[ "$dispatch_release" == true ]]; then
  printf 'Dispatching GitHub Release workflow:\n'
  printf '  %q' "${release_cmd[@]}"
  printf '\n'
  "${release_cmd[@]}"
fi

if [[ "$dispatch_central" == true ]]; then
  printf 'Dispatching Maven Central workflow:\n'
  printf '  %q' "${central_cmd[@]}"
  printf '\n'
  "${central_cmd[@]}"
fi
