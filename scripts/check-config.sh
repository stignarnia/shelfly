#!/usr/bin/env bash
# Check the three things that go wrong silently, because nothing downstream complains when they do.
#
#   Tag    - a tag whose name disagrees with versionName ships a mislabelled release.
#            CI fires on v* tags and publishes release_notes.txt as the release body, so tagging v4.0.7 while versions.gradle.kts still says 4.0.6 passes every other check and produces a release that claims to be something it is not.
#            Skipped when HEAD carries no tag, which is every ordinary commit.
#
#   Locale - resourceConfigurations in app/build.gradle.kts pins the locales that survive into the APK.
#            Adding res/values-nb without adding 'nb' to that list strips the translation at build time, and Lint stays quiet: MissingTranslation only reasons about locales that are already configured, so a complete, correct, silently discarded translation looks exactly like a healthy one.
#            The reverse - a configured locale with no resources anywhere - is listed too, since it is dead configuration.
#
#   Unused - a resource nothing references is invisible to Lint here, so it accumulates instead of being caught.
#            UnusedResources only reports meaningfully in an application module, and 28 of the 29 modules are libraries - Lint has to assume a consumer it cannot see might use anything a library declares.
#            viewBinding hides the rest, generating a binding class per layout that Lint counts as a use, so an orphaned layout looks alive no matter how long nothing has inflated it.
#            Resolving references across every module at once is sound here precisely because the module graph is closed: nothing outside this repository consumes these resources.
#
#   scripts/check-config.sh
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

failed=0

# Tag.
version="$(sed -n 's/.*extra\["versionName"\][[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' versions.gradle.kts | head -n 1)"
if [ -z "$version" ]; then
  echo "error: could not read versionName from versions.gradle.kts" >&2
  exit 1
fi

tag="$(git tag --points-at HEAD | head -n 1)"
if [ -z "$tag" ]; then
  echo "tag: HEAD is not tagged, skipping (versionName is $version)"
else
  # Both v4.0.6 and 4.0.6 are accepted, matching the two tag patterns the workflow triggers on.
  if [ "$tag" != "v$version" ] && [ "$tag" != "$version" ]; then
    echo "error: tag $tag does not match versionName $version from versions.gradle.kts." >&2
    echo "The release would be published under a version the app does not report." >&2
    failed=1
  else
    echo "tag: $tag matches versionName $version"
  fi
fi

# Locale.
# Read across lines: the list is one locale per line, so a line-based match would see only the first.
configured="$(awk '/resourceConfigurations/,/^[[:space:]]*\)/' app/build.gradle.kts | grep -oE '"[a-z]{2}"' | tr -d '"' | sort -u)"
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
  echo "Add them to resourceConfigurations in app/build.gradle.kts, or delete the resources." >&2
  failed=1
fi

if [ -n "$missing_from_tree" ]; then
  echo "error: these locales are in resourceConfigurations but have no resources anywhere:" >&2
  echo "$missing_from_tree" | sed 's/^/  /' >&2
  failed=1
fi

echo "locales: $(echo "$configured" | wc -l) configured, all present, none stripped"

# Unused.
# Types declared by a file's own name, as res/<type>/<name>.<ext>, rather than by an element inside res/values.
file_types='layout|drawable|mipmap|anim|animator|menu|raw|xml|color|font|transition|interpolator'
# A file staged for deletion, or deleted in the working tree but not yet staged, is still listed by git ls-files.
# Everything below reads file contents, so drop what is not on disk rather than letting grep fail the run.
existing() { while IFS= read -r f; do [ -f "$f" ] && printf '%s\n' "$f"; done; }

# Files that can carry a reference. R8 rules are included because keep rules name resources too.
searchable="$(git ls-files --cached --others --exclude-standard '*.xml' '*.kt' '*.java' '*.gradle' '*.pro' | existing)"
values_files="$(git ls-files --cached --others --exclude-standard '*/res/values*/*.xml' | existing)"

declared="$( {
  # File-based: the name is the filename, the type is the directory before any qualifier.
  git ls-files --cached --others --exclude-standard | existing | sed -nE "s#.*/res/($file_types)(-[^/]*)?/([^/]+)\.[a-z]+\$#\1/\3#p"

  if [ -n "$values_files" ]; then
    # Element-based: <dimen name="x"/>, <style name="Y">, and friends.
    echo "$values_files" | xargs grep -hoE '<(string|color|dimen|style|bool|integer|attr|plurals|string-array|integer-array|array|declare-styleable|fraction)[[:space:]][^>]*name="[^"]+"' \
      | sed -E 's|<([a-z-]+)[[:space:]][^>]*name="([^"]+)".*|\1/\2|' \
      | sed -E 's#^(string-array|integer-array)/#array/#; s#^declare-styleable/#styleable/#'

    # <item name="x" type="dimen"> also declares. An <item> inside a <style> carries no type attribute, so it is not caught here.
    echo "$values_files" | xargs grep -hoE '<item[[:space:]][^>]*name="[^"]+"[^>]*type="[^"]+"' \
      | sed -E 's|<item[[:space:]][^>]*name="([^"]+)"[^>]*type="([^"]+)".*|\2/\1|'
  fi
} | sort -u)"

referenced="$( {
  # @drawable/foo and ?attr/bar. Framework resources carry an android: prefix and are skipped - they are not ours to declare.
  echo "$searchable" | xargs grep -hoE '[@?][a-z]+/[A-Za-z0-9_.]+' | sed -E 's|^[@?]||'
  # The namespace-less theme attribute form, ?colorAccent.
  echo "$searchable" | xargs grep -hoE '\?[A-Za-z][A-Za-z0-9_]*' | sed -E 's|^\?|attr/|'
  # R.layout.foo from code.
  echo "$searchable" | xargs grep -hoE '\bR\.[a-z]+\.[A-Za-z0-9_.]+' | sed -E 's|^R\.||; s|^([a-z]+)\.|\1/|'
  # A declare-styleable attribute reaches code as R.styleable.MyView_icon, never as @attr/icon.
  echo "$searchable" | xargs grep -hoE '\bR\.styleable\.[A-Za-z0-9]+_[A-Za-z0-9_]+' | sed -E 's|.*_([A-Za-z0-9_]+)$|attr/\1|'
  # A style parent may be named without the @style/ prefix.
  echo "$searchable" | xargs grep -hoE 'parent="(@style/)?[A-Za-z0-9_.]+"' | sed -E 's|parent="(@style/)?||; s|"$||; s|^|style/|'
  # A style named Foo.Bar implicitly extends Foo, so declaring the child uses the parent.
  echo "$declared" | sed -nE 's|^style/(.+)\.[^.]+$|style/\1|p'
  # A layout is reachable through its generated binding class as well as through R.layout.
  join -j 1 -o 1.2 \
    <(echo "$declared" | sed -nE 's|^layout/(.+)$|\1|p' \
        | awk '{n=split($0,p,"_"); c=""; for(i=1;i<=n;i++) c=c toupper(substr(p[i],1,1)) substr(p[i],2); print c "Binding layout/" $0}' \
        | sort -k1,1) \
    <(echo "$searchable" | grep -vE '/res/' | xargs grep -hoE '\b[A-Za-z0-9]+Binding\b' | sort -u)
} | sort -u)"

unused="$(comm -23 <(echo "$declared") <(echo "$referenced"))"

if [ -n "$unused" ]; then
  echo "error: $(echo "$unused" | wc -l) resource(s) are declared but referenced nowhere in the project:" >&2
  echo "$unused" | sed 's/^/  /' >&2
  echo "Delete them, or reference them. Deleting one can orphan what it referenced, so re-run until this passes." >&2
  failed=1
else
  echo "resources: $(echo "$declared" | wc -l) declared, all referenced"
fi

if [ "$failed" -ne 0 ]; then
  exit 1
fi
