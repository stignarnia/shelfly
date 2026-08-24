#!/usr/bin/env bash
# Check that the release notes shown in the What's New screen match the version being shipped.
#
# CLAUDE.md treats release_notes.txt as part of "done" rather than a release-time chore, but nothing enforced that until now.
# A stale heading ships the previous version's notes to users, which is silent - the app renders whatever the file says.
#
# Verifies two things:
#   - the first line is "Shelfly <versionName>", with versionName read from versions.gradle.kts.
#   - at least one note follows the heading, so a version bump cannot ship an empty list.
#
#   scripts/check-release-notes.sh
#
# Exits non-zero with an explanation when either check fails.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
versions_file="$root/versions.gradle.kts"
notes_file="$root/app/src/main/assets/release_notes.txt"

for f in "$versions_file" "$notes_file"; do
  if [[ ! -f "$f" ]]; then
    echo "error: expected file not found: $f" >&2
    exit 1
  fi
done

# versions.gradle.kts holds the single source of truth, as 'extra["versionName"] = "4.0.6"'.
version="$(sed -n 's/.*extra\["versionName"\][[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$versions_file" | head -n 1)"
if [[ -z "$version" ]]; then
  echo "error: could not read versionName from $versions_file" >&2
  exit 1
fi

expected="Shelfly $version"
# Strip a trailing carriage return so a file saved with CRLF endings does not fail for invisible reasons.
actual="$(head -n 1 "$notes_file" | tr -d '\r')"

if [[ "$actual" != "$expected" ]]; then
  echo "error: release notes heading is out of sync with versions.gradle.kts." >&2
  echo "  expected: $expected" >&2
  echo "  found:    $actual" >&2
  echo "" >&2
  echo "Update the first line of app/src/main/assets/release_notes.txt and start a fresh list beneath it." >&2
  exit 1
fi

# A heading on its own means the version was bumped but the notes were never written.
notes_count="$(tail -n +2 "$notes_file" | tr -d '\r' | grep -c '[^[:space:]]' || true)"
if [[ "$notes_count" -eq 0 ]]; then
  echo "error: $expected has no notes beneath it." >&2
  echo "Add one bullet per user-visible change, describing what is different in the app." >&2
  exit 1
fi

echo "release notes OK: $expected ($notes_count line(s))"
