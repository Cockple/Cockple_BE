#!/bin/bash
# 사용법: bash scripts/tunnel.sh [GCP_IP]
# 예시:   bash scripts/tunnel.sh 34.64.xxx.xxx

GCP_IP=${1}

if [ -z "$GCP_IP" ]; then
  read -p "GCP IP 입력: " GCP_IP
fi

echo "터널링 시작: $GCP_IP"
echo "  MySQL  -> localhost:3306 -> cockple-mysql:3306"
echo "  Redis  -> localhost:6379 -> cockple-redis:6379"
echo "종료: Ctrl+C"

ssh -N \
  -L 3307:localhost:3306 \
  -L 6380:localhost:6379 \
  -i ~/.ssh/cockple_gcp \
  ubuntu@$GCP_IP
