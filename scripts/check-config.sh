#!/usr/bin/env bash
# Check the two project settings that go wrong silently, because nothing downstream complains when they do.
#
#   Tag    - a tag whose name disagrees with versionName ships a mislabelled release.
#            CI fires on v* tags and publishes release_notes.txt as the release body, so tagging v4.0.7 while versions.gradle still says 4.0.6 passes every other check and produces a release that claims to be something it is not.
#            Skipped when HEAD carries no tag, which is every ordinary commit.
#
#   Locale - resourceConfigurations in app/build.gradle pins the locales that survive into the APK.
#            Adding res/values-nb without adding 'nb' to that list strips the translation at build time, and Lint stays quiet: MissingTranslation only reasons about locales that are already configured, so a complete, correct, silently discarded translation looks exactly like a healthy one.
#            The reverse - a configured locale with no resources anywhere - is listed too, since it is dead configuration.
#
#   scripts/check-config.sh
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

failed=0

# Tag.
version="$(sed -n "s/.*versionName[[:space:]]*:[[:space:]]*'\([^']*\)'.*/\1/p" versions.gradle | head -n 1)"
if [ -z "$version" ]; then
  echo "error: could not read versionName from versions.gradle" >&2
  exit 1
fi

tag="$(git tag --points-at HEAD | head -n 1)"
if [ -z "$tag" ]; then
  echo "tag: HEAD is not tagged, skipping (versionName is $version)"
else
  # Both v4.0.6 and 4.0.6 are accepted, matching the two tag patterns the workflow triggers on.
  if [ "$tag" != "v$version" ] && [ "$tag" != "$version" ]; then
    echo "error: tag $tag does not match versionName $version from versions.gradle." >&2
    echo "The release would be published under a version the app does not report." >&2
    failed=1
  else
    echo "tag: $tag matches versionName $version"
  fi
fi

# Locale.
configured="$(grep -oP "resourceConfigurations \+= \[\K[^]]*" app/build.gradle | tr -d "' " | tr ',' '\n' | grep -v '^$' | sort -u)"
if [ -z "$configured" ]; then
  echo "error: could not read resourceConfigurations from app/build.gradle" >&2
  exit 1
fi

# Locale-qualified directories only: night, v34, sw600dp and friends are configuration qualifiers, not languages.
# 'en' is added by hand: the default res/values holds the English strings, so there is no values-en directory to find.
present="$( {
  printf 'en\n'
  find . -type d -name 'values-*' -not -path '*/build/*' 2>/dev/null \
    | sed 's|.*/values-||' \
    | grep -vE '^(night|night-v[0-9]+|v[0-9]+|sw[0-9]+dp|land|port)$'
} | sort -u)"

missing_from_config="$(comm -13 <(echo "$configured") <(echo "$present"))"
missing_from_tree="$(comm -23 <(echo "$configured") <(echo "$present"))"

if [ -n "$missing_from_config" ]; then
  echo "error: these locales have resources but are not in resourceConfigurations, so they are stripped from the APK:" >&2
  echo "$missing_from_config" | sed 's/^/  /' >&2
  echo "Add them to resourceConfigurations in app/build.gradle, or delete the resources." >&2
  failed=1
fi

if [ -n "$missing_from_tree" ]; then
  echo "error: these locales are in resourceConfigurations but have no resources anywhere:" >&2
  echo "$missing_from_tree" | sed 's/^/  /' >&2
  failed=1
fi

if [ "$failed" -ne 0 ]; then
  exit 1
fi

echo "locales: $(echo "$configured" | wc -l) configured, all present, none stripped"
