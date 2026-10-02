#!/usr/bin/env bash
set -euo pipefail

APK="${1:-$GITHUB_WORKSPACE/step35-artifacts/app/build/outputs/apk/release/app-release.apk}"

test -s "$APK"
chmod +x "$GITHUB_WORKSPACE/.github/scripts/runtime_apk_smoke_gate.sh"
chmod +x "$GITHUB_WORKSPACE/.github/scripts/runtime_50_screen_ultra_gate.sh"

"$GITHUB_WORKSPACE/.github/scripts/runtime_apk_smoke_gate.sh" "$APK"
"$GITHUB_WORKSPACE/.github/scripts/runtime_50_screen_ultra_gate.sh" "$APK"

echo "SIGNED APK RUNTIME 50-SCREEN HEAVY GATE: PASS"
