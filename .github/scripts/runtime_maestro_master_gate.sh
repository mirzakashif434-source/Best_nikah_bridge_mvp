#!/usr/bin/env bash
set -euo pipefail

APK="${1:?APK path required}"
ROOT="${GITHUB_WORKSPACE:-$(pwd)}"

bash "$ROOT/.github/scripts/runtime_apk_smoke_gate.sh" "$APK"
bash "$ROOT/.github/scripts/runtime_50_screen_ultra_gate.sh" "$APK"

MAESTRO="${HOME}/.maestro/bin/maestro"
test -x "$MAESTRO"
"$MAESTRO" --version

wait_for_adb() {
  adb start-server >/dev/null 2>&1 || true
  for _ in $(seq 1 30); do
    if adb get-state 2>/dev/null | grep -qx device; then
      return 0
    fi
    sleep 2
  done
  return 1
}

run_maestro_once() {
  local log="$1"
  set +e
  "$MAESTRO" test "$ROOT/.maestro/ci-welcome.yaml" 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  set -e
  return "$rc"
}

wait_for_adb || {
  echo "ADB device unavailable before Maestro."
  exit 86
}

LOG1="$(mktemp)"
if run_maestro_once "$LOG1"; then
  echo "MAESTRO CI WELCOME/ACCESSIBILITY SMOKE: PASS"
  echo "RUNTIME + 50-SCREEN ULTRA + MAESTRO MASTER GATE: PASS"
  exit 0
fi

# Retry only for infrastructure/device transport loss. Genuine UI assertion
# failures are never hidden and fail immediately.
if grep -Eqi "device .* not found|Connection refused|could not connect to TCP port|host:transport:.*device" "$LOG1"; then
  echo "MAESTRO INFRA SELF-HEAL: ADB transport lost; reconnecting once."
  adb kill-server >/dev/null 2>&1 || true
  sleep 2
  adb start-server >/dev/null 2>&1 || true
  if wait_for_adb; then
    LOG2="$(mktemp)"
    if run_maestro_once "$LOG2"; then
      echo "MAESTRO CI WELCOME/ACCESSIBILITY SMOKE: PASS AFTER ADB SELF-HEAL"
      echo "RUNTIME + 50-SCREEN ULTRA + MAESTRO MASTER GATE: PASS"
      exit 0
    fi
  fi
  echo "MAESTRO INFRA SELF-HEAL: device did not recover on current emulator."
  exit 86
fi

echo "MAESTRO REAL TEST FAILURE: not an ADB transport issue."
exit 1
