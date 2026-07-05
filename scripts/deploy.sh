#!/bin/bash
set -euo pipefail

DOCKER_REPO="${1:?DOCKER_REPO is required}"
BRANCH="${2:?BRANCH is required}"

cd /home/ubuntu/cockple

if [ "$BRANCH" == "main" ]; then
  SERVICE="cockple-app"
  TAG="latest"
  INSTALL_PROD_BACKUP="true"
else
  SERVICE="cockple-app-staging"
  TAG="staging"
  INSTALL_PROD_BACKUP="false"
fi

cat > .env << EOF
DB_PASSWORD=${DB_PASSWORD}
GCS_BUCKET=${GCS_BUCKET}
KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
KAKAO_REDIRECT_URI_PROD=${KAKAO_REDIRECT_URI_PROD}
KAKAO_REDIRECT_URI_STAGING=${KAKAO_REDIRECT_URI_STAGING}
KAKAO_ADMIN_KEY=${KAKAO_ADMIN_KEY}
JWT_SECRET_KEY=${JWT_SECRET_KEY}
FIREBASE_SERVICE_ACCOUNT_KEY=${FIREBASE_SERVICE_ACCOUNT_KEY}
FCM_FAKE_LATENCY_MS=${FCM_FAKE_LATENCY_MS:-0}
EOF

echo "${FIREBASE_SERVICE_ACCOUNT_KEY}" > /home/ubuntu/cockple/firebase-service-account.json

mkdir -p /home/ubuntu/cockple/logs/prod /home/ubuntu/cockple/logs/staging

echo "=== 배포 전 상태 ==="
sudo docker ps

sudo docker compose up -d mysql redis nginx
sudo docker image prune -f
sudo docker pull $DOCKER_REPO:$TAG

sudo docker stop $SERVICE || true
sudo docker rm -f $SERVICE || true

sudo docker compose up -d $SERVICE

echo "=== 배포 후 상태 ==="
sudo docker ps

echo "=== 헬스체크 ==="
for container in cockple-mysql cockple-redis $SERVICE; do
  for i in $(seq 1 12); do
    STATUS=$(sudo docker inspect --format='{{.State.Status}}' $container 2>/dev/null)
    HEALTH=$(sudo docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $container 2>/dev/null)

    if [ "$STATUS" != "running" ]; then
      echo "FAIL: $container 상태 이상 (status=$STATUS)"
      sudo docker logs --tail 20 $container
      exit 1
    fi

    if [ "$HEALTH" == "healthy" ] || [ "$HEALTH" == "none" ]; then
      echo "OK: $container (status=$STATUS, health=$HEALTH)"
      break
    fi

    if [ $i -eq 12 ]; then
      echo "FAIL: $container 헬스체크 타임아웃 (health=$HEALTH)"
      sudo docker logs --tail 20 $container
      exit 1
    fi

    echo "대기 중: $container ($i/12, health=$HEALTH)..."
    sleep 5
  done
done

if [ "${INSTALL_PROD_BACKUP}" == "true" ]; then
  : "${GCS_BACKUP_BUCKET:?GCS_BACKUP_BUCKET is required for prod backup setup}"

  echo "=== 운영 DB 백업 systemd timer 설치 ==="
  RUN_BACKUP_SMOKE_TEST=true \
    GCS_BACKUP_BUCKET="${GCS_BACKUP_BUCKET}" \
    bash /home/ubuntu/cockple/scripts/install_backup_systemd.sh
else
  echo "=== staging 배포: 운영 DB 백업 runtime은 건드리지 않음 ==="
fi

echo "=== 배포 성공 ==="
