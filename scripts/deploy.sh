#!/bin/bash

DOCKER_REPO=$1
BRANCH=$2

cd /home/ubuntu/cockple

if [ "$BRANCH" == "main" ]; then
  SERVICE="cockple-app"
  TAG="latest"
else
  SERVICE="cockple-app-staging"
  TAG="staging"
fi

cat > .env << EOF
DB_PASSWORD=${DB_PASSWORD}
S3_BUCKET=${S3_BUCKET}
S3_ACCESS_KEY=${S3_ACCESS_KEY}
S3_SECRET_KEY=${S3_SECRET_KEY}
KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
KAKAO_REDIRECT_URI_PROD=${KAKAO_REDIRECT_URI_PROD}
KAKAO_REDIRECT_URI_STAGING=${KAKAO_REDIRECT_URI_STAGING}
KAKAO_ADMIN_KEY=${KAKAO_ADMIN_KEY}
JWT_SECRET_KEY=${JWT_SECRET_KEY}
EOF

echo "=== 배포 전 상태 ==="
sudo docker ps

sudo docker image prune -f
sudo docker pull $DOCKER_REPO:$TAG

sudo docker stop $SERVICE || true
sudo docker rm -f $SERVICE || true

if ! sudo docker ps | grep -q cockple-mysql; then
  echo "MySQL이 죽었음, 재시작 중..."
  sudo docker compose up -d mysql
  sleep 15
fi

if ! sudo docker ps | grep -q cockple-redis; then
  echo "Redis가 죽었음, 재시작 중..."
  sudo docker compose up -d redis
  sleep 5
fi

sudo docker compose up -d $SERVICE

echo "=== 배포 후 상태 ==="
sudo docker ps
