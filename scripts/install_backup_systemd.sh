#!/bin/bash
set -euo pipefail

readonly BACKUP_ROOT="/opt/cockple/backup"
readonly BACKUP_ENV_FILE="/etc/cockple/db-backup.env"
readonly BACKUP_STATE_DIR="/var/lib/cockple/db-backups"
readonly LEGACY_CRON_FILE="/etc/cron.d/cockple-db-backup"
readonly SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SERVICE_SOURCE="${BACKUP_ROOT}/cockple-db-backup.service"
readonly TIMER_SOURCE="${BACKUP_ROOT}/cockple-db-backup.timer"
readonly SERVICE_TARGET="/etc/systemd/system/cockple-db-backup.service"
readonly TIMER_TARGET="/etc/systemd/system/cockple-db-backup.timer"

if [[ "${EUID}" -eq 0 ]]; then
  SUDO=()
else
  SUDO=(sudo)
fi

require_file() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "Required file not found: ${path}" >&2
    exit 1
  fi
}

ensure_curl_installed() {
  if command -v curl >/dev/null 2>&1; then
    return
  fi

  "${SUDO[@]}" apt-get update -y
  "${SUDO[@]}" apt-get install -y ca-certificates curl
}

install_systemd_units() {
  "${SUDO[@]}" install -m 0644 "${SERVICE_SOURCE}" "${SERVICE_TARGET}"
  "${SUDO[@]}" install -m 0644 "${TIMER_SOURCE}" "${TIMER_TARGET}"

  "${SUDO[@]}" systemctl daemon-reload
  "${SUDO[@]}" systemctl enable --now cockple-db-backup.timer
}

run_backup_smoke_test() {
  if "${SUDO[@]}" systemctl start cockple-db-backup.service; then
    return
  fi

  "${SUDO[@]}" systemctl status --no-pager cockple-db-backup.service || true
  "${SUDO[@]}" journalctl -u cockple-db-backup.service -n 80 --no-pager || true
  exit 1
}

cleanup_legacy_cron() {
  if [[ -f "${LEGACY_CRON_FILE}" ]]; then
    "${SUDO[@]}" rm -f "${LEGACY_CRON_FILE}"
  fi
}

stage_backup_runtime() {
  "${SUDO[@]}" install -m 0755 -d "${BACKUP_ROOT}" "$(dirname "${BACKUP_ENV_FILE}")" "$(dirname "${BACKUP_STATE_DIR}")"
  "${SUDO[@]}" install -m 0700 -d "${BACKUP_STATE_DIR}"
  "${SUDO[@]}" install -m 0755 "${SOURCE_DIR}/backup_db.sh" "${BACKUP_ROOT}/backup_db.sh"
  "${SUDO[@]}" install -m 0755 "${SOURCE_DIR}/run_db_backup.sh" "${BACKUP_ROOT}/run_db_backup.sh"
  "${SUDO[@]}" install -m 0644 "${SOURCE_DIR}/cockple-db-backup.service" "${BACKUP_ROOT}/cockple-db-backup.service"
  "${SUDO[@]}" install -m 0644 "${SOURCE_DIR}/cockple-db-backup.timer" "${BACKUP_ROOT}/cockple-db-backup.timer"
}

write_backup_env() {
  if [[ -n "${GCS_BACKUP_BUCKET:-}" ]]; then
    "${SUDO[@]}" install -m 0644 /dev/null "${BACKUP_ENV_FILE}"
    cat <<EOF | "${SUDO[@]}" tee "${BACKUP_ENV_FILE}" >/dev/null
GCS_BACKUP_BUCKET=${GCS_BACKUP_BUCKET}
BACKUP_DATABASE=cockple
GCS_OBJECT_PREFIX=prod
LOCAL_RETENTION_DAYS=2
EOF
    return
  fi

  if [[ ! -f "${BACKUP_ENV_FILE}" ]]; then
    echo "GCS_BACKUP_BUCKET is required when ${BACKUP_ENV_FILE} does not exist." >&2
    exit 1
  fi
}

require_file "${SOURCE_DIR}/backup_db.sh"
require_file "${SOURCE_DIR}/run_db_backup.sh"
require_file "${SOURCE_DIR}/cockple-db-backup.service"
require_file "${SOURCE_DIR}/cockple-db-backup.timer"

stage_backup_runtime
write_backup_env
ensure_curl_installed
cleanup_legacy_cron

require_file "${BACKUP_ENV_FILE}"
require_file "${BACKUP_ROOT}/backup_db.sh"
require_file "${BACKUP_ROOT}/run_db_backup.sh"
require_file "${SERVICE_SOURCE}"
require_file "${TIMER_SOURCE}"

install_systemd_units

if [[ "${RUN_BACKUP_SMOKE_TEST:-false}" == "true" ]]; then
  run_backup_smoke_test
fi

echo "Backup systemd timer installed."
