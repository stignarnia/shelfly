#!/usr/bin/env bash
# Stream live logs from the debug build on the attached device.
#
# Scoped to the app's own process, so it shows Timber output and the OkHttp request/response bodies that debug builds enable, without the system noise a keyword grep over the full buffer drags in.
#
#   scripts/logcat.sh            # follow live
#   scripts/logcat.sh omdb       # follow live, only lines matching a pattern
#
# Note the app must be running: the PID is resolved once, at startup.
set -euo pipefail

PACKAGE="${SHELFLY_PACKAGE:-xyz.stignarnia.shelfly.debugoss}"

pid="$(adb shell pidof "$PACKAGE" | tr -d '\r' | awk '{print $1}')"
if [[ -z "$pid" ]]; then
  echo "error: $PACKAGE is not running. Launch it first:" >&2
  echo "  adb shell monkey -p $PACKAGE -c android.intent.category.LAUNCHER 1" >&2
  exit 1
fi

echo "streaming $PACKAGE (pid $pid)${1:+, filtered on /$1/} - ctrl-c to stop" >&2
if [[ $# -gt 0 ]]; then
  adb logcat -v time --pid="$pid" | grep --line-buffered -iE "$1"
else
  adb logcat -v time --pid="$pid"
fi
