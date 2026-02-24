#!/bin/bash

# GCP 서버를 통해 Docker MySQL/Redis 터널링
# 사용법: ./scripts/tunnel.sh [GCP_IP]
# 예시:   ./scripts/tunnel.sh 34.64.xxx.xxx

GCP_IP=${1:-$(cat .tunnel-ip 2>/dev/null)}

if [ -z "$GCP_IP" ]; then
  echo "GCP IP를 인자로 전달하거나 .tunnel-ip 파일에 저장하세요."
  echo "사용법: ./scripts/tunnel.sh [GCP_IP]"
  exit 1
fi

echo "터널링 시작: $GCP_IP"
echo "  MySQL  → localhost:3306 → cockple-mysql:3306"
echo "  Redis  → localhost:6379 → cockple-redis:6379"
echo "종료: Ctrl+C"

ssh -N \
  -L 3306:localhost:3306 \
  -L 6379:localhost:6379 \
  -i ~/.ssh/cockple \
  ubuntu@$GCP_IP
