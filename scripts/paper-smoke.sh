#!/usr/bin/env bash
# Boot a headless Paper 26.3 server with the built plugin and require a clean enable.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PAPER_VERSION="${PAPER_VERSION:-26.3}"
PAPER_BUILD="${PAPER_BUILD:-258}"
USER_AGENT="${PAPER_USER_AGENT:-deathban-pro-smoke/2.0 (https://github.com/KevinTCoughlin/deathban-pro)}"
STARTUP_TIMEOUT="${PAPER_SMOKE_TIMEOUT:-150}"

java_major="$(java -version 2>&1 | awk -F'[".]' '/version/ { print $2; exit }')"
if [[ "${java_major}" -lt 25 ]]; then
  echo "Paper ${PAPER_VERSION} smoke test requires Java 25 or newer (found ${java_major})." >&2
  exit 1
fi

plugin_jar="${1:-}"
if [[ -z "${plugin_jar}" ]]; then
  mapfile -t jars < <(find "${ROOT}/build/libs" -maxdepth 1 -type f -name 'DeathBanPro-*.jar' | sort)
  if [[ ${#jars[@]} -ne 1 ]]; then
    echo "Expected one DeathBanPro jar in build/libs, found ${#jars[@]}." >&2
    printf '  %s\n' "${jars[@]:-}" >&2
    exit 1
  fi
  plugin_jar="${jars[0]}"
fi
if [[ ! -f "${plugin_jar}" ]]; then
  echo "Plugin jar not found: ${plugin_jar}" >&2
  exit 1
fi

if [[ -n "${PAPER_SMOKE_DIR:-}" ]]; then
  work="${PAPER_SMOKE_DIR}"
  mkdir -p "${work}"
else
  work="$(mktemp -d "${TMPDIR:-/tmp}/deathban-paper.XXXXXX")"
fi

log="${work}/smoke.log"
: >"${log}"
failed=0
server_pid=""
server_in=""

cleanup() {
  local status=$?
  if [[ -n "${server_pid}" ]] && kill -0 "${server_pid}" 2>/dev/null; then
    if [[ -n "${server_in}" ]]; then
      echo stop >&"${server_in}" 2>/dev/null || true
    fi
    local _i
    for _i in $(seq 1 30); do
      kill -0 "${server_pid}" 2>/dev/null || break
      sleep 1
    done
    kill "${server_pid}" 2>/dev/null || true
    wait "${server_pid}" 2>/dev/null || true
  fi
  if [[ "${failed}" -ne 0 || "${status}" -ne 0 ]]; then
    echo "----- Paper smoke log (last 200 lines) -----" >&2
    tail -n 200 "${log}" >&2 || true
  fi
  if [[ "${PAPER_SMOKE_KEEP:-0}" != 1 && -z "${PAPER_SMOKE_DIR:-}" ]]; then
    rm -rf "${work}"
  fi
}
trap cleanup EXIT

mkdir -p "${work}/plugins"
cp "${plugin_jar}" "${work}/plugins/DeathBanPro.jar"
rm -f "${work}/world/session.lock"
printf 'eula=true\n' >"${work}/eula.txt"
cat >"${work}/server.properties" <<'EOF'
online-mode=false
server-port=25595
motd=deathban-smoke
level-name=world
level-type=minecraft:flat
generate-structures=false
spawn-monsters=false
spawn-animals=false
spawn-npcs=false
view-distance=2
simulation-distance=2
max-players=1
spawn-protection=0
sync-chunk-writes=false
EOF

meta="$(curl -fsSL -A "${USER_AGENT}" "https://fill.papermc.io/v3/projects/paper/versions/${PAPER_VERSION}/builds/${PAPER_BUILD}")"
paper_url="$(printf '%s' "${meta}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])')"
paper_sha="$(printf '%s' "${meta}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["checksums"]["sha256"])')"
paper_jar="${work}/paper-${PAPER_VERSION}-${PAPER_BUILD}.jar"
if [[ ! -f "${paper_jar}" ]] || ! echo "${paper_sha}  ${paper_jar}" | sha256sum -c --status; then
  curl -fsSL -A "${USER_AGENT}" -o "${paper_jar}.partial" "${paper_url}"
  mv "${paper_jar}.partial" "${paper_jar}"
  echo "${paper_sha}  ${paper_jar}" | sha256sum -c --status
fi

cd "${work}"
coproc SERVER (
  java -Xms512M -Xmx1024M -Dcom.mojang.eula.agree=true -jar "${paper_jar}" --nogui >"${log}" 2>&1
)
server_pid="${SERVER_PID}"
server_in="${SERVER[1]}"

deadline=$((SECONDS + STARTUP_TIMEOUT))
booted=0
while ((SECONDS < deadline)); do
  if ! kill -0 "${server_pid}" 2>/dev/null; then
    echo "Paper exited before DeathBan Pro finished enabling." >&2
    failed=1
    exit 1
  fi
  if grep -q "Error occurred while enabling DeathBanPro" "${log}" ||
    grep -q "Invalid config.yml" "${log}" ||
    grep -q "requires Java 25 and Paper 26.3" "${log}"; then
    echo "DeathBan Pro failed during Paper startup." >&2
    failed=1
    exit 1
  fi
  if grep -Eq "DeathBan Pro v.+ enabled!" "${log}" && grep -q "Done (" "${log}"; then
    booted=1
    break
  fi
  sleep 2
done

if [[ "${booted}" -ne 1 ]]; then
  echo "Timed out after ${STARTUP_TIMEOUT}s waiting for DeathBan Pro to enable." >&2
  failed=1
  exit 1
fi

echo stop >&"${server_in}"
stop_deadline=$((SECONDS + 40))
while ((SECONDS < stop_deadline)); do
  if ! kill -0 "${server_pid}" 2>/dev/null; then
    break
  fi
  sleep 1
done
if kill -0 "${server_pid}" 2>/dev/null; then
  echo "Paper did not stop after the smoke check." >&2
  failed=1
  exit 1
fi
wait "${server_pid}" || {
  echo "Paper exited with a failure status after DeathBan Pro enabled." >&2
  failed=1
  exit 1
}
echo "DeathBan Pro enabled on Paper ${PAPER_VERSION} build ${PAPER_BUILD}."
