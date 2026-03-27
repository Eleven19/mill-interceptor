#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/ci/recommend-prerelease.sh

Recommend the next prerelease version based on:
- the latest stable vX.Y.Z tag
- conventional commits since that tag
- existing prerelease tags on the recommended base version

The script prints a human-readable summary and an exact helper command for
dispatching both release workflows for the recommended prerelease. Set
OUTPUT=json for machine-readable output.
EOF
}

if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

output_format="${OUTPUT:-text}"

stable_tag_pattern='^v([0-9]+)\.([0-9]+)\.([0-9]+)$'
prerelease_tag_pattern='^v([0-9]+\.[0-9]+\.[0-9]+)-([0-9A-Za-z-]+)\.([0-9]+)$'
breaking_subject_pattern='^[[:alpha:]][[:alnum:]-]*(\([^)]*\))?!:'
feature_subject_pattern='^feat(\([^)]*\))?:'
fix_subject_pattern='^fix(\([^)]*\))?:'

latest_stable_tag="$(
  git tag --list 'v*' --sort=-version:refname |
    while IFS= read -r tag; do
      if [[ "$tag" =~ $stable_tag_pattern ]]; then
        printf '%s\n' "$tag"
        break
      fi
    done
)"

if [[ -z "$latest_stable_tag" ]]; then
  echo "Unable to determine the latest stable tag matching vX.Y.Z" >&2
  exit 1
fi

latest_stable_version="${latest_stable_tag#v}"
IFS='.' read -r stable_major stable_minor stable_patch <<<"$latest_stable_version"

current_branch="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$current_branch" == "HEAD" ]]; then
  current_branch="$(git rev-parse --short HEAD)"
fi

sanitize_branch_name() {
  local branch_name="$1"
  branch_name="${branch_name#refs/heads/}"
  branch_name="$(printf '%s' "$branch_name" | tr '[:upper:]' '[:lower:]')"
  branch_name="$(printf '%s' "$branch_name" | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//; s/-{2,}/-/g')"
  if [[ -z "$branch_name" ]]; then
    branch_name="branch"
  fi
  printf '%s\n' "$branch_name"
}

branch_channel="$(sanitize_branch_name "$current_branch")"

log_range="${latest_stable_tag}..HEAD"
commit_subjects=()
while IFS= read -r subject; do
  if [[ -n "$subject" ]]; then
    commit_subjects+=("$subject")
  fi
done < <(git log --format=%s "$log_range")

if ((${#commit_subjects[@]} == 0)); then
  echo "No commits found since $latest_stable_tag" >&2
  exit 1
fi

breaking_count=0
feature_count=0
fix_count=0
other_count=0
breaking_reason=""
feature_reason=""

while IFS= read -r body_line; do
  if [[ "$body_line" == *"BREAKING CHANGE:"* || "$body_line" == *"BREAKING-CHANGE:"* ]]; then
    ((breaking_count += 1))
    if [[ -z "$breaking_reason" ]]; then
      breaking_reason="$body_line"
    fi
  fi
done < <(git log --format=%B "$log_range")

for subject in "${commit_subjects[@]}"; do
  if [[ "$subject" =~ $breaking_subject_pattern ]]; then
    ((breaking_count += 1))
    if [[ -z "$breaking_reason" ]]; then
      breaking_reason="$subject"
    fi
    continue
  fi

  if [[ "$subject" =~ $feature_subject_pattern ]]; then
    ((feature_count += 1))
    if [[ -z "$feature_reason" ]]; then
      feature_reason="$subject"
    fi
  elif [[ "$subject" =~ $fix_subject_pattern ]]; then
    ((fix_count += 1))
  else
    ((other_count += 1))
  fi
done

recommended_bump="patch"
recommended_reason="no breaking or feat commits detected since ${latest_stable_tag}"

if ((breaking_count > 0)); then
  recommended_bump="major"
  recommended_reason="breaking change detected: ${breaking_reason:-${commit_subjects[0]}}"
elif ((feature_count > 0)); then
  recommended_bump="minor"
  recommended_reason="feat commit detected: ${feature_reason:-${commit_subjects[0]}}"
fi

next_version_for_bump() {
  local bump="$1"
  local major="$stable_major"
  local minor="$stable_minor"
  local patch="$stable_patch"

  case "$bump" in
    major)
      printf '%s.0.0\n' "$((major + 1))"
      ;;
    minor)
      printf '%s.%s.0\n' "$major" "$((minor + 1))"
      ;;
    patch)
      printf '%s.%s.%s\n' "$major" "$minor" "$((patch + 1))"
      ;;
    *)
      echo "Unsupported bump: $bump" >&2
      exit 1
      ;;
  esac
}

recommended_base_version="$(next_version_for_bump "$recommended_bump")"
patch_base_version="$(next_version_for_bump patch)"
minor_base_version="$(next_version_for_bump minor)"
major_base_version="$(next_version_for_bump major)"

next_prerelease_number() {
  local base_version="$1"
  local channel="$2"
  local max_number=0
  local tag version prerelease_channel prerelease_number

  while IFS= read -r tag; do
    if [[ "$tag" =~ $prerelease_tag_pattern ]]; then
      version="${BASH_REMATCH[1]}"
      prerelease_channel="${BASH_REMATCH[2]}"
      prerelease_number="${BASH_REMATCH[3]}"
      if [[ "$version" == "$base_version" && "$prerelease_channel" == "$channel" ]]; then
        if ((prerelease_number > max_number)); then
          max_number="$prerelease_number"
        fi
      fi
    fi
  done < <(git tag --list 'v*')

  printf '%s\n' "$((max_number + 1))"
}

build_version() {
  local base_version="$1"
  local channel="$2"
  local next_number
  next_number="$(next_prerelease_number "$base_version" "$channel")"
  printf '%s-%s.%s\n' "$base_version" "$channel" "$next_number"
}

recommended_rc_version="$(build_version "$recommended_base_version" rc)"
recommended_beta_version="$(build_version "$recommended_base_version" beta)"
recommended_alpha_version="$(build_version "$recommended_base_version" alpha)"
recommended_branch_version="$(build_version "$recommended_base_version" "$branch_channel")"

patch_rc_version="$(build_version "$patch_base_version" rc)"
minor_rc_version="$(build_version "$minor_base_version" rc)"
major_rc_version="$(build_version "$major_base_version" rc)"

recommended_command="scripts/ci/dispatch-release-suite.sh --ref ${current_branch} --version ${recommended_rc_version}"

if [[ "$output_format" == "json" ]]; then
  escaped_reason="$(printf '%s' "$recommended_reason" | sed 's/"/\\"/g')"
  cat <<EOF
{
  "latest_stable_tag": "${latest_stable_tag}",
  "latest_stable_version": "${latest_stable_version}",
  "current_branch": "${current_branch}",
  "branch_channel": "${branch_channel}",
  "commit_counts": {
    "breaking": ${breaking_count},
    "feat": ${feature_count},
    "fix": ${fix_count},
    "other": ${other_count}
  },
  "recommended_bump": "${recommended_bump}",
  "recommended_reason": "${escaped_reason}",
  "recommended_base_version": "${recommended_base_version}",
  "recommended_version": "${recommended_rc_version}",
  "alternatives": {
    "beta": "${recommended_beta_version}",
    "alpha": "${recommended_alpha_version}",
    "branch": "${recommended_branch_version}",
    "patch_rc": "${patch_rc_version}",
    "minor_rc": "${minor_rc_version}",
    "major_rc": "${major_rc_version}"
  },
  "dispatch_command": "${recommended_command}"
}
EOF
  exit 0
fi

cat <<EOF
Latest stable tag: ${latest_stable_tag}
Current branch: ${current_branch}
Branch prerelease channel: ${branch_channel}

Conventional commit summary since ${latest_stable_tag}:
- breaking: ${breaking_count}
- feat: ${feature_count}
- fix: ${fix_count}
- other: ${other_count}

Recommended base bump: ${recommended_bump}
Reason: ${recommended_reason}

Recommended prerelease:
- rc: ${recommended_rc_version}

Alternative channels on ${recommended_base_version}:
- beta: ${recommended_beta_version}
- alpha: ${recommended_alpha_version}
- branch: ${recommended_branch_version}

Alternative release lines:
- patch rc: ${patch_rc_version}
- minor rc: ${minor_rc_version}
- major rc: ${major_rc_version}

Dispatch command:
${recommended_command}
EOF
