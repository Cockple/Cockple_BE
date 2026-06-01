#!/bin/bash
set -euo pipefail

readonly BACKUP_ENV_FILE="/etc/cockple/db-backup.env"
readonly BACKUP_DIR="/var/lib/cockple/db-backups"
readonly LOCK_FILE="${BACKUP_DIR}/.db-backup.lock"
readonly DB_BACKUP_SCRIPT="/opt/cockple/backup/backup_db.sh"

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

umask 077
mkdir -p "${BACKUP_DIR}"

exec 9>"${LOCK_FILE}"
if ! flock -n 9; then
  echo "Backup already in progress. Skipping."
  exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is not installed." >&2
  exit 1
fi

if [[ ! "${GCS_BACKUP_BUCKET}" =~ ^[a-z0-9][a-z0-9._-]{1,220}[a-z0-9]$ ]]; then
  echo "Invalid GCS_BACKUP_BUCKET: ${GCS_BACKUP_BUCKET}" >&2
  exit 1
fi

if [[ ! "${GCS_OBJECT_PREFIX}" =~ ^[A-Za-z0-9._/-]+$ || "${GCS_OBJECT_PREFIX}" == /* || "${GCS_OBJECT_PREFIX}" == */ ]]; then
  echo "Invalid GCS_OBJECT_PREFIX: ${GCS_OBJECT_PREFIX}" >&2
  exit 1
fi

urlencode() {
  local raw="$1"
  local length=${#raw}
  local i
  local char

  for ((i = 0; i < length; i++)); do
    char="${raw:i:1}"
    case "${char}" in
      [a-zA-Z0-9.~_-]) printf '%s' "${char}" ;;
      *) printf '%%%02X' "'${char}" ;;
    esac
  done
}

get_gcp_access_token() {
  local token_json
  local token

  if token_json="$(curl -fsS --connect-timeout 2 \
    -H "Metadata-Flavor: Google" \
    "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token" 2>/dev/null)"; then
    token="$(printf '%s' "${token_json}" | sed -n 's/.*"access_token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
    if [[ -n "${token}" ]]; then
      printf '%s\n' "${token}"
      return 0
    fi
  fi

  if command -v gcloud >/dev/null 2>&1; then
    gcloud auth print-access-token
    return
  fi

  echo "Could not get a GCP access token from the metadata server, and gcloud is unavailable." >&2
  return 1
}

upload_backup_to_gcs() {
  local local_path="$1"
  local object_path="$2"
  local encoded_object_path
  local upload_url
  local token
  local response_file
  local http_code

  encoded_object_path="$(urlencode "${object_path}")"
  upload_url="https://storage.googleapis.com/upload/storage/v1/b/${GCS_BACKUP_BUCKET}/o?uploadType=media&name=${encoded_object_path}&ifGenerationMatch=0"
  token="$(get_gcp_access_token)"
  response_file="$(mktemp)"

  echo "Uploading backup to gs://${GCS_BACKUP_BUCKET}/${object_path}"

  if ! http_code="$(curl -sS -o "${response_file}" -w '%{http_code}' \
    -X POST \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/gzip" \
    --data-binary "@${local_path}" \
    "${upload_url}")"; then
    echo "GCS upload request failed before receiving an HTTP response." >&2
    cat "${response_file}" >&2 || true
    rm -f "${response_file}"
    return 1
  fi

  if [[ ! "${http_code}" =~ ^2 ]]; then
    echo "GCS upload failed with HTTP ${http_code}." >&2
    cat "${response_file}" >&2 || true
    rm -f "${response_file}"
    return 1
  fi

  rm -f "${response_file}"
}

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

upload_backup_to_gcs "${LOCAL_BACKUP_PATH}" "${GCS_OBJECT_PATH}"

rm -f "${LOCAL_BACKUP_PATH}"

echo "Backup uploaded to gs://${GCS_BACKUP_BUCKET}/${GCS_OBJECT_PATH}"
echo "=== DB backup completed: ${TIMESTAMP_UTC} ==="
