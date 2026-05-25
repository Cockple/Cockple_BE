#!/bin/bash
set -euo pipefail

readonly CONTAINER_NAME="cockple-mysql"

DATABASE_NAME="${1:-}"
OUTPUT_PATH="${2:-}"

if [[ -z "${DATABASE_NAME}" || -z "${OUTPUT_PATH}" ]]; then
  echo "Usage: $0 <database_name> <output_path>" >&2
  exit 1
fi

if [[ ! "${DATABASE_NAME}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "Invalid database name: ${DATABASE_NAME}" >&2
  exit 1
fi

if [[ "${EUID}" -eq 0 ]]; then
  DOCKER_CMD=(docker)
else
  DOCKER_CMD=(sudo docker)
fi

if ! "${DOCKER_CMD[@]}" inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
  echo "MySQL container not found: ${CONTAINER_NAME}" >&2
  exit 1
fi

CONTAINER_STATUS="$("${DOCKER_CMD[@]}" inspect --format='{{.State.Status}}' "${CONTAINER_NAME}")"
if [[ "${CONTAINER_STATUS}" != "running" ]]; then
  echo "MySQL container is not running: status=${CONTAINER_STATUS}" >&2
  exit 1
fi

OUTPUT_DIR="$(dirname "${OUTPUT_PATH}")"
mkdir -p "${OUTPUT_DIR}"

TMP_OUTPUT="${OUTPUT_PATH}.tmp"
rm -f "${TMP_OUTPUT}"
umask 077

cleanup_tmp() {
  rm -f "${TMP_OUTPUT}"
}

trap cleanup_tmp EXIT

"${DOCKER_CMD[@]}" exec "${CONTAINER_NAME}" sh -c '
  db_name="$1"
  exec mysqldump \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --events \
    --set-gtid-purged=OFF \
    --default-character-set=utf8mb4 \
    --databases "$db_name" \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD"
' sh "${DATABASE_NAME}" | gzip -c > "${TMP_OUTPUT}"

if [[ ! -s "${TMP_OUTPUT}" ]]; then
  echo "Backup file was not created or is empty: ${TMP_OUTPUT}" >&2
  exit 1
fi

mv "${TMP_OUTPUT}" "${OUTPUT_PATH}"
trap - EXIT
echo "Backup created: ${OUTPUT_PATH}"
