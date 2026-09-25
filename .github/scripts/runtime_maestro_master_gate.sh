#!/usr/bin/env bash
set -euo pipefail

APK="${1:?APK path required}"
ROOT="${GITHUB_WORKSPACE:-$(pwd)}"

bash "$ROOT/.github/scripts/runtime_apk_smoke_gate.sh" "$APK"

MAESTRO="${HOME}/.maestro/bin/maestro"
test -x "$MAESTRO"
"$MAESTRO" --version
"$MAESTRO" test "$ROOT/.maestro/ci-welcome.yaml"

echo "MAESTRO CI WELCOME/ACCESSIBILITY SMOKE: PASS"
echo "RUNTIME + MAESTRO MASTER GATE: PASS"
