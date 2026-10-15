#!/usr/bin/env bash
# Compatibility smoke runner (skeleton).
#
# This script is the entry point a live-server runner uses. It is deliberately explicit about its
# inputs so a report can record exactly what was tested. It does NOT download anything by default:
# the loader, Minecraft and fixtures must be provisioned by the caller (see integration-tests/README.md).
#
# Usage:
#   integration-tests/scripts/run-compat-smoke.sh <loader:fabric|neoforge> <cellulosesz-jar> <run-dir> [fixture-jar ...]
set -euo pipefail

LOADER="${1:?loader required: fabric or neoforge}"
CZ_JAR="${2:?cellulosesz distribution jar required}"
RUN_DIR="${3:?clean run directory required}"
shift 3

if [[ "$LOADER" != "fabric" && "$LOADER" != "neoforge" ]]; then
  echo "unknown loader: $LOADER" >&2
  exit 2
fi

if [[ ! -f "$CZ_JAR" ]]; then
  echo "missing CellulosesZ jar: $CZ_JAR" >&2
  exit 2
fi

mkdir -p "$RUN_DIR/mods"
cp "$CZ_JAR" "$RUN_DIR/mods/"
for fixture in "$@"; do
  if [[ ! -f "$fixture" ]]; then
    echo "missing fixture jar: $fixture" >&2
    exit 2
  fi
  cp "$fixture" "$RUN_DIR/mods/"
done

cat <<EOF
Prepared $RUN_DIR for a $LOADER live-server smoke.
  - CellulosesZ: $CZ_JAR
  - fixtures: ${*:-none}

Next: start the $LOADER dedicated server in $RUN_DIR, wait for a verifiable ready signal, then drive
the scenario assertions and write a report matching integration-tests/expected/compat-report.schema.json.
This harness intentionally does not fetch loaders or accept the EULA automatically.
EOF
