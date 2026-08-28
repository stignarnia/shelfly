#!/usr/bin/env bash
# Run ktlint, downloading it first if the working tree does not have it.
#
# ktlint is a self-executing jar rather than a Gradle plugin, so no Gradle task runs it and `check` will never catch a formatting violation - it has to be invoked separately.
# The jar is gitignored, so this wrapper exists to make a fresh clone work without anyone reading the README first.
#
#   scripts/ktlint.sh              # check
#   scripts/ktlint.sh --format     # check and rewrite
#
# Every argument is passed straight through to ktlint.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# shellcheck source-path=SCRIPTDIR
# shellcheck source=lib/tools.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/tools.sh"

ensure_ktlint

exec ./ktlint "$@"
