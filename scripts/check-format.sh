#!/usr/bin/env bash
# Check whitespace hygiene in the tracked text files ktlint does not read.
#
# ktlint covers .kt and .kts and nothing else, so everything below had no automated check at all.
#
# The glob list is the scope, and it is deliberately explicit rather than "everything else":
# binaries (.png, .webp, .jar, fonts) are excluded because the rules are meaningless for them, and gradlew / gradlew.bat are excluded because Gradle regenerates them - a wrapper upgrade that emitted a tab would otherwise fail a build over a vendor file nobody should hand-edit.
#
# This is a whitespace check, not a formatter.
# It will not reindent XML, reorder attributes, or wrap long lines; real formatting would need xmllint --format or an editorconfig-checker binary, which are heavier and pull in a download.
#
# Three rules, all of which the tree already satisfies, so this holds a clean state rather than starting a cleanup:
#   - no CRLF line endings.
#   - no hard tabs.
#   - no trailing whitespace.
#
# insert_final_newline is deliberately NOT enforced.
# 335 of the 934 tracked files would fail it today, and normalising them means a mechanical commit across most of the resource tree.
# .editorconfig declares it instead, so editors fix each file as it is genuinely edited and the count falls without a blame-wrecking sweep.
#
#   scripts/check-format.sh
#
# Only tracked files are scanned, so build output and generated sources are excluded for free.
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

GLOBS=(
  '*.gradle.kts' '*.xml' '*.yml' '*.yaml' '*.pro' '*.sh' '*.properties'
  '*.toml' '*.json' '*.md' '*.txt'
  '.editorconfig' '.gitignore' '*/.gitignore' 'LICENSE'
)

crlf=()
tabs=()
trailing=()

while IFS= read -r file; do
  [ -f "$file" ] || continue
  if grep -qU $'\r' "$file" 2>/dev/null; then crlf+=("$file"); fi
  if grep -qP '\t' "$file" 2>/dev/null; then tabs+=("$file"); fi
  if grep -qP '[ \t]+$' "$file" 2>/dev/null; then trailing+=("$file"); fi
done < <(git ls-files -- "${GLOBS[@]}")

scanned=$(git ls-files -- "${GLOBS[@]}" | wc -l)
failed=0

report() {
  local label="$1"
  shift
  local files=("$@")
  if [ "${#files[@]}" -gt 0 ]; then
    failed=1
    echo "error: $label (${#files[@]} file(s)):" >&2
    printf '  %s\n' "${files[@]}" >&2
  fi
}

report "CRLF line endings" ${crlf+"${crlf[@]}"}
report "hard tabs" ${tabs+"${tabs[@]}"}
report "trailing whitespace" ${trailing+"${trailing[@]}"}

if [ "$failed" -ne 0 ]; then
  echo "" >&2
  echo "Fix with: sed -i 's/[[:space:]]*\$//' <file>   (trailing whitespace)" >&2
  echo "          sed -i 's/\\t/  /g' <file>            (tabs)" >&2
  exit 1
fi

echo "format OK: $scanned file(s), no CRLF, tabs, or trailing whitespace"
