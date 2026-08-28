#!/usr/bin/env bash
# Check the tracked text files ktlint does not read.
#
# Three things happen here, and only the first is whitespace.
#
#   Whitespace - delegated to editorconfig-checker, which reads .editorconfig directly.
#                Checking it here rather than reimplementing the rules keeps one definition: a second copy in this script could disagree with the file the editor reads, and nothing would catch it.
#
#   Shell      - shellcheck over every tracked script and both git hooks.
#                These scripts are what check everything else, and nothing checked them: .editorconfig covers their whitespace and stops there, so a syntax error or an unquoted expansion only ever surfaced when someone ran one.
#
#   XML        - well-formedness, which nothing else in the build checks.
#                aapt only parses the resources of the variant being built, so a malformed file in a locale or qualifier that variant skips is not read at all until some device configuration selects it.
#
#   scripts/check-format.sh
#
# Only tracked files are scanned, so build output, generated sources and local.properties are excluded for free.
# The file list is passed to ec explicitly rather than letting it walk the working tree, so its default excludes never have to agree with .gitignore.
# Files that .editorconfig declares no rules for - gradlew, gradlew.bat, the ktlint jar, every binary - match no section and are reported on by nothing, so no exclude list is needed.
#
# There is no line length limit in .editorconfig, so nothing here checks one.
# Comments are one sentence per line and sentences are never wrapped to fit a column.
# -e is deliberately absent so every check below runs and the output lists all of them, rather than stopping at the first.
# That makes the cd its own failure case: without -e a failed one would leave the rest of the script running against the wrong directory.
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.." || exit 1

# The linters are downloaded on demand rather than being prerequisites, so a fresh clone runs this with no setup.
# shellcheck source-path=SCRIPTDIR
# shellcheck source=lib/tools.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/tools.sh"

ensure_editorconfig_checker || exit 1
ensure_shellcheck || exit 1

# xmllint is the one thing that cannot be fetched this way - it is a system package rather than a single release binary.
# A missing one is an error rather than a skip, because a check that quietly does nothing is worse than one that is absent.
if ! command -v xmllint >/dev/null 2>&1; then
  echo "error: xmllint not found. Install libxml2-utils (Debian/Ubuntu) or libxml2 (Arch)." >&2
  exit 1
fi

failed=0

# editorconfig-checker reports but does not rewrite - it has no --fix, only --dry-run.
# Saving the file in an editor that reads .editorconfig is the intended fix; .vscode/settings.json applies these rules on save.
if ! git ls-files -z | xargs -0 ./editorconfig-checker; then
  echo "" >&2
  echo "Open the file and save it - .vscode/settings.json applies these rules on save. In bulk:" >&2
  printf '%s\n' \
    "  sed -i 's/[[:space:]]*\$//' <file>              (trailing whitespace)" \
    "  sed -i 's/\t/  /g' <file>                      (tabs)" \
    "  sed -i 's/\r\$//' <file>                        (CRLF)" \
    "  tail -c1 <file> | read -r _ || printf '\n' >> <file>   (missing final newline)" >&2
  failed=1
fi

# The scripts that check everything else were themselves unchecked until now.
# -x so the sourced lib/tools.sh is read as part of whatever sources it, rather than its functions being treated as undefined.
# The hooks have no extension, so git ls-files cannot glob them and they are named explicitly.
mapfile -t sh_files < <(git ls-files -- '*.sh')
if ! ./shellcheck -x "${sh_files[@]}" scripts/hooks/pre-commit scripts/hooks/commit-msg; then
  failed=1
fi

malformed=()
while IFS= read -r file; do
  [ -f "$file" ] || continue
  xmllint --noout "$file" >/dev/null 2>&1 || malformed+=("$file")
done < <(git ls-files -- '*.xml')

if [ "${#malformed[@]}" -gt 0 ]; then
  echo "error: malformed XML (${#malformed[@]} file(s)):" >&2
  printf '  %s\n' "${malformed[@]}" >&2
  failed=1
fi

if [ "$failed" -ne 0 ]; then
  exit 1
fi

echo "format OK: $(git ls-files | wc -l) tracked file(s) match .editorconfig; $(git ls-files -- '*.xml' | wc -l) XML file(s) well formed; ${#sh_files[@]} shell script(s) pass shellcheck"
