#!/usr/bin/env bash
# Fetch the two linters that are not Gradle plugins.
#
# Both live in the repository root and are gitignored, so a fresh clone has neither and a working tree never carries a binary.
# Nothing is installed system-wide: no package manager, no sudo, no PATH changes.
#
# Neither is pinned.
# Tracking the latest release means a rule the upstream tool adds is caught the next time anyone runs it, rather than whenever someone remembers to bump a version - and a pin in CI that drifts from what everyone runs locally is worse than no pin at all.
#
# This is sourced rather than executed, and CI calls the same scripts that source it, so the download happens in exactly one place for both.
#
#   source "$(dirname "${BASH_SOURCE[0]}")/lib/tools.sh"
#   ensure_ktlint
#   ensure_editorconfig_checker

# editorconfig-checker publishes per-platform tarballs, so the asset name has to be built rather than guessed.
ec_asset() {
  local os arch
  case "$(uname -s)" in
    Linux) os=linux ;;
    Darwin) os=darwin ;;
    *)
      echo "error: unsupported OS $(uname -s) for editorconfig-checker" >&2
      return 1
      ;;
  esac
  case "$(uname -m)" in
    x86_64 | amd64) arch=amd64 ;;
    aarch64 | arm64) arch=arm64 ;;
    *)
      echo "error: unsupported architecture $(uname -m) for editorconfig-checker" >&2
      return 1
      ;;
  esac
  printf 'ec-%s-%s' "$os" "$arch"
}

ensure_editorconfig_checker() {
  [ -x ./editorconfig-checker ] && return 0

  local asset
  asset="$(ec_asset)" || return 1

  echo "editorconfig-checker not present, fetching the latest release..." >&2
  # -O so a partial download cannot be left behind as an executable that fails in a confusing way later.
  if ! curl -sSLf "https://github.com/editorconfig-checker/editorconfig-checker/releases/latest/download/${asset}.tar.gz" \
    | tar xz -O "bin/${asset}" > editorconfig-checker.tmp; then
    rm -f editorconfig-checker.tmp
    echo "error: could not download editorconfig-checker." >&2
    return 1
  fi
  chmod a+x editorconfig-checker.tmp
  mv editorconfig-checker.tmp editorconfig-checker
  echo "editorconfig-checker $(./editorconfig-checker --version) ready" >&2
}

ensure_ktlint() {
  [ -x ./ktlint ] && return 0

  echo "ktlint not present, fetching the latest release..." >&2
  if ! curl -sSLf -o ktlint.tmp https://github.com/pinterest/ktlint/releases/latest/download/ktlint; then
    rm -f ktlint.tmp
    echo "error: could not download ktlint." >&2
    return 1
  fi
  chmod a+x ktlint.tmp
  mv ktlint.tmp ktlint
  echo "ktlint ready" >&2
}
