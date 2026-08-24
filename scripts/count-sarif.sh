#!/usr/bin/env bash
# Count Android Lint findings across all generated SARIF reports in the project.
#
# Parses every build/reports/lint-results-debug.sarif in the workspace and summarizes issues by module.
#
#   scripts/count-sarif.sh
#
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

sarif_files=()
while IFS= read -r f; do
  [ -n "$f" ] && sarif_files+=("$f")
done < <(find . -path '*/build/reports/lint-results-debug.sarif' | sort)

if [ ${#sarif_files[@]} -eq 0 ]; then
  echo "No SARIF files found. Run ./gradlew lintDebug first."
  exit 0
fi

total_issues=0
files_temp=$(mktemp)
trap 'rm -f "$files_temp"' EXIT

declare -A module_issues

for sf in "${sarif_files[@]}"; do
  mod=$(echo "$sf" | sed -E 's|^\./([^/]+)/.*|\1|')
  if command -v jq >/dev/null 2>&1; then
    issues=$(jq '[.runs[].results[]] | length' "$sf" 2>/dev/null || echo 0)
    jq -r --arg m "$mod" '.runs[].results[].locations[].physicalLocation.artifactLocation.uri // empty | "\($m)\t\(.)"' "$sf" 2>/dev/null >> "$files_temp" || true
  else
    issues=$(grep -c '"ruleId"' "$sf" || echo 0)
  fi

  if [ "$issues" -gt 0 ]; then
    module_issues["$mod"]=$(( ${module_issues["$mod"]:-0} + issues ))
  fi
  total_issues=$(( total_issues + issues ))
done

total_files=$(cut -f2 "$files_temp" 2>/dev/null | sort -u | grep -v '^$' | wc -l || echo 0)

echo "TOTAL SARIF FILES CHECKED: ${#sarif_files[@]}"
echo "TOTAL ISSUES: $total_issues"
echo "TOTAL FILES WITH ISSUES: $total_files"
echo ""
echo "BREAKDOWN BY MODULE:"
for mod in $(echo "${!module_issues[@]}" | tr ' ' '\n' | sort); do
  mod_files=$(awk -F'\t' -v m="$mod" '$1 == m { print $2 }' "$files_temp" | sort -u | grep -v '^$' | wc -l || echo 0)
  echo "- $mod: ${module_issues[$mod]} issues across ${mod_files} files"
done
