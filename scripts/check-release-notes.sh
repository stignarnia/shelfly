#!/usr/bin/env bash
# Check that the release notes shown in the What's New screen match the version being shipped.
#
# CLAUDE.md treats release_notes.txt as part of "done" rather than a release-time chore, but nothing enforced that until now.
# A stale heading ships the previous version's notes to users, which is silent - the app renders whatever the file says.
#
# Verifies three things:
#   - the first line is "Shelfly <versionName>", with versionName read from gradle/libs.versions.toml.
#   - at least one note follows the heading, so a version bump cannot ship an empty list.
#   - every locale in androidResources.localeFilters has a release_notes-<locale>.txt beside the English file, with the same heading and the same number of notes.
#     The count is what catches a note added in English and never translated: the app would otherwise show that language the previous, shorter list without complaint.
#
#   scripts/check-release-notes.sh
#
# Exits non-zero with an explanation when either check fails.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
versions_file="$root/gradle/libs.versions.toml"
notes_file="$root/app/src/main/assets/release_notes.txt"

for f in "$versions_file" "$notes_file"; do
  if [[ ! -f "$f" ]]; then
    echo "error: expected file not found: $f" >&2
    exit 1
  fi
done

# The catalog holds the single source of truth, as 'versionName = "4.0.6"' under [versions].
version="$(sed -n 's/^versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$versions_file" | head -n 1)"
if [[ -z "$version" ]]; then
  echo "error: could not read versionName from $versions_file" >&2
  exit 1
fi

expected="Shelfly $version"
# Strip a trailing carriage return so a file saved with CRLF endings does not fail for invisible reasons.
actual="$(head -n 1 "$notes_file" | tr -d '\r')"

if [[ "$actual" != "$expected" ]]; then
  echo "error: release notes heading is out of sync with gradle/libs.versions.toml." >&2
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

# Translations.
# Read across lines, as check-config.sh does: the list is one locale per line.
locales="$(awk '/localeFilters/,/^[[:space:]]*\)/' "$root/app/build.gradle.kts" | grep -oE '"[a-z]{2}"' | tr -d '"' | grep -vx en | sort -u)"
if [[ -z "$locales" ]]; then
  echo "error: could not read androidResources.localeFilters from app/build.gradle.kts" >&2
  exit 1
fi

failed=0
for locale in $locales; do
  translated="$root/app/src/main/assets/release_notes-$locale.txt"
  if [[ ! -f "$translated" ]]; then
    echo "error: app/src/main/assets/release_notes-$locale.txt is missing." >&2
    failed=1
    continue
  fi
  translated_heading="$(head -n 1 "$translated" | tr -d '\r')"
  if [[ "$translated_heading" != "$expected" ]]; then
    echo "error: release_notes-$locale.txt is headed '$translated_heading', expected '$expected'." >&2
    failed=1
  fi
  translated_count="$(tail -n +2 "$translated" | tr -d '\r' | grep -c '[^[:space:]]' || true)"
  if [[ "$translated_count" -ne "$notes_count" ]]; then
    echo "error: release_notes-$locale.txt has $translated_count note line(s), the English file has $notes_count." >&2
    failed=1
  fi
done

if [[ "$failed" -ne 0 ]]; then
  echo "" >&2
  echo "Every language shows its own release_notes-<locale>.txt in the What's New screen, so each one has to be updated alongside the English file." >&2
  exit 1
fi

echo "release notes OK: $expected ($notes_count line(s), $(wc -w <<< "$locales") translation(s))"
