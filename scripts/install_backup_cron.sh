#!/bin/bash
set -euo pipefail

readonly APP_DIR="/home/ubuntu/cockple"
readonly BACKUP_ENV_FILE="${APP_DIR}/.backup.env"
readonly CRON_FILE="/etc/cron.d/cockple-db-backup"
readonly LOG_DIR="${APP_DIR}/logs"
readonly RUN_SCRIPT="${APP_DIR}/scripts/run_db_backup.sh"

require_file() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "Required file not found: ${path}" >&2
    exit 1
  fi
}

ensure_cron_installed() {
  if ! dpkg -s cron >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo apt-get install -y cron
  fi

  sudo systemctl enable cron
  sudo systemctl restart cron
}

ensure_gcloud_installed() {
  if command -v gcloud >/dev/null 2>&1; then
    return
  fi

  sudo apt-get update -y
  sudo apt-get install -y apt-transport-https ca-certificates gnupg curl

  if [[ ! -f /usr/share/keyrings/cloud.google.gpg ]]; then
    curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg \
      | sudo gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg
  fi

  if [[ ! -f /etc/apt/sources.list.d/google-cloud-sdk.list ]]; then
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" \
      | sudo tee /etc/apt/sources.list.d/google-cloud-sdk.list >/dev/null
  fi

  sudo apt-get update -y
  sudo apt-get install -y google-cloud-cli
}

install_cron_file() {
  sudo tee "${CRON_FILE}" >/dev/null <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
CRON_TZ=Asia/Seoul

15 3 * * * ubuntu cd ${APP_DIR} && /bin/bash ${RUN_SCRIPT} >> ${LOG_DIR}/db-backup-cron.log 2>&1
EOF

  sudo chmod 644 "${CRON_FILE}"
}

mkdir -p "${LOG_DIR}" "${APP_DIR}/backups"

require_file "${BACKUP_ENV_FILE}"
require_file "${RUN_SCRIPT}"

ensure_cron_installed
ensure_gcloud_installed
install_cron_file

echo "Backup cron installed at ${CRON_FILE}"
