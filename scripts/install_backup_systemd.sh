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

ensure_gcloud_installed() {
  if command -v gcloud >/dev/null 2>&1; then
    return
  fi

  "${SUDO[@]}" apt-get update -y
  "${SUDO[@]}" apt-get install -y apt-transport-https ca-certificates gnupg curl

  if [[ ! -f /usr/share/keyrings/cloud.google.gpg ]]; then
    curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg \
      | "${SUDO[@]}" gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg
  fi

  if [[ ! -f /etc/apt/sources.list.d/google-cloud-sdk.list ]]; then
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" \
      | "${SUDO[@]}" tee /etc/apt/sources.list.d/google-cloud-sdk.list >/dev/null
  fi

  "${SUDO[@]}" apt-get update -y
  "${SUDO[@]}" apt-get install -y google-cloud-cli
}

install_systemd_units() {
  "${SUDO[@]}" install -m 0644 "${SERVICE_SOURCE}" "${SERVICE_TARGET}"
  "${SUDO[@]}" install -m 0644 "${TIMER_SOURCE}" "${TIMER_TARGET}"

  "${SUDO[@]}" systemctl daemon-reload
  "${SUDO[@]}" systemctl enable --now cockple-db-backup.timer
}

cleanup_legacy_cron() {
  if [[ -f "${LEGACY_CRON_FILE}" ]]; then
    "${SUDO[@]}" rm -f "${LEGACY_CRON_FILE}"
  fi
}

stage_backup_runtime() {
  "${SUDO[@]}" install -m 0755 -d "${BACKUP_ROOT}" "$(dirname "${BACKUP_ENV_FILE}")" "${BACKUP_STATE_DIR}"
  "${SUDO[@]}" install -m 0755 "${SOURCE_DIR}/backup_db.sh" "${BACKUP_ROOT}/backup_db.sh"
  "${SUDO[@]}" install -m 0755 "${SOURCE_DIR}/run_db_backup.sh" "${BACKUP_ROOT}/run_db_backup.sh"
  "${SUDO[@]}" install -m 0644 "${SOURCE_DIR}/cockple-db-backup.service" "${BACKUP_ROOT}/cockple-db-backup.service"
  "${SUDO[@]}" install -m 0644 "${SOURCE_DIR}/cockple-db-backup.timer" "${BACKUP_ROOT}/cockple-db-backup.timer"
}

write_backup_env() {
  if [[ -n "${GCS_BACKUP_BUCKET:-}" ]]; then
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
ensure_gcloud_installed
cleanup_legacy_cron

require_file "${BACKUP_ENV_FILE}"
require_file "${BACKUP_ROOT}/backup_db.sh"
require_file "${BACKUP_ROOT}/run_db_backup.sh"
require_file "${SERVICE_SOURCE}"
require_file "${TIMER_SOURCE}"

install_systemd_units

if [[ "${RUN_BACKUP_SMOKE_TEST:-false}" == "true" ]]; then
  "/bin/bash" "${BACKUP_ROOT}/run_db_backup.sh"
fi

echo "Backup systemd timer installed."
