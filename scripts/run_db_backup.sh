#!/bin/bash
set -euo pipefail

readonly APP_DIR="/home/ubuntu/cockple"
readonly BACKUP_ENV_FILE="${APP_DIR}/.backup.env"
readonly BACKUP_DIR="${APP_DIR}/backups"
readonly LOG_DIR="${APP_DIR}/logs"
readonly LOCK_FILE="${BACKUP_DIR}/.db-backup.lock"
readonly DB_BACKUP_SCRIPT="${APP_DIR}/scripts/backup_db.sh"

if [[ ! -f "${BACKUP_ENV_FILE}" ]]; then
  echo "Backup config not found: ${BACKUP_ENV_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${BACKUP_ENV_FILE}"

: "${GCS_BACKUP_BUCKET:?GCS_BACKUP_BUCKET is required}"

BACKUP_DATABASE="${BACKUP_DATABASE:-cockple}"
GCS_OBJECT_PREFIX="${GCS_OBJECT_PREFIX:-prod}"
LOCAL_RETENTION_DAYS="${LOCAL_RETENTION_DAYS:-2}"

mkdir -p "${BACKUP_DIR}" "${LOG_DIR}"

exec 9>"${LOCK_FILE}"
if ! flock -n 9; then
  echo "Backup already in progress. Skipping."
  exit 0
fi

if ! command -v gcloud >/dev/null 2>&1; then
  echo "gcloud CLI is not installed." >&2
  exit 1
fi

TIMESTAMP_UTC="$(date -u +'%Y-%m-%dT%H-%M-%SZ')"
YEAR_UTC="$(date -u +'%Y')"
MONTH_UTC="$(date -u +'%m')"
FILE_NAME="${BACKUP_DATABASE}-${TIMESTAMP_UTC}.sql.gz"
LOCAL_BACKUP_PATH="${BACKUP_DIR}/${FILE_NAME}"
GCS_OBJECT_PATH="${GCS_OBJECT_PREFIX}/${YEAR_UTC}/${MONTH_UTC}/${FILE_NAME}"

cleanup_stale_files() {
  find "${BACKUP_DIR}" -maxdepth 1 -type f -name '*.tmp' -mtime +1 -delete || true
  find "${BACKUP_DIR}" -maxdepth 1 -type f -name '*.sql.gz' -mtime +"${LOCAL_RETENTION_DAYS}" -delete || true
}

cleanup_stale_files

echo "=== DB backup started: ${TIMESTAMP_UTC} ==="
"${DB_BACKUP_SCRIPT}" "${BACKUP_DATABASE}" "${LOCAL_BACKUP_PATH}"

gzip -t "${LOCAL_BACKUP_PATH}"

gcloud storage cp "${LOCAL_BACKUP_PATH}" "gs://${GCS_BACKUP_BUCKET}/${GCS_OBJECT_PATH}"

rm -f "${LOCAL_BACKUP_PATH}"

echo "Backup uploaded to gs://${GCS_BACKUP_BUCKET}/${GCS_OBJECT_PATH}"
echo "=== DB backup completed: ${TIMESTAMP_UTC} ==="
