#!/usr/bin/env bash
# Guard the exported Room schemas in data-local/schemas.
#
# Those files are what MigrationsTest validates against, so they are only as trustworthy as their history.
# Two distinct ways they go wrong, and both are checked here:
#
#   Drift   - the build regenerated a schema that was never committed, so the repository describes a database that no longer matches the entities.
#   History - an already-released schema was edited in place instead of a new version being added.
#
# The history check is the one that closes the hole.
# Drift alone cannot: editing 45.json and committing the result looks perfectly clean, while every user who already installed 45 has the old shape on disk and MigrationsTest now validates against a file that describes nobody's database.
#
#   scripts/check-schemas.sh
#
# The drift check reads the working tree, so run it after a build that regenerated the schemas - anything that compiles data-local, such as testDebugUnitTest.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

SCHEMA_DIR="data-local/schemas"

if [ ! -d "$SCHEMA_DIR" ]; then
  echo "error: $SCHEMA_DIR does not exist" >&2
  exit 1
fi

# Drift.
# --porcelain rather than "git diff --exit-code" so a brand new schema, which is untracked rather than modified, is caught too.
drift="$(git status --porcelain -- "$SCHEMA_DIR")"
if [ -n "$drift" ]; then
  echo "error: exported schemas differ from what is committed." >&2
  echo "$drift" >&2
  echo "" >&2
  echo "The Room compiler regenerated these. Commit them - they are build output worth keeping, and MigrationsTest validates against them." >&2
  exit 1
fi

# History.
# Compare against the last release rather than the previous commit, so the check describes what users actually have installed.
baseline="$(git describe --tags --abbrev=0 2>/dev/null || true)"
if [ -z "$baseline" ]; then
  echo "no tags found, skipping the history check (drift check passed)"
  echo "CI needs fetch-depth: 0 for tags to be present."
  exit 0
fi

# Anything other than an addition means a schema that shipped in $baseline was changed or removed.
offenders="$(git diff --name-status "$baseline" HEAD -- "$SCHEMA_DIR" | grep -v '^A' || true)"
if [ -n "$offenders" ]; then
  echo "error: schemas released in $baseline were modified rather than superseded." >&2
  echo "$offenders" >&2
  echo "" >&2
  echo "A shipped schema describes a database that already exists on users' devices and cannot be rewritten." >&2
  echo "Bump DATABASE_VERSION, add a migration, and let Room export a new file instead." >&2
  exit 1
fi

added="$(git diff --name-status "$baseline" HEAD -- "$SCHEMA_DIR" | grep -c '^A' || true)"
echo "schemas OK: no drift, $added added since $baseline, none modified"
