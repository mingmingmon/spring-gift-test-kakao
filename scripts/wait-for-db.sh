#!/bin/bash
set -e

MAX_RETRIES=30
RETRY_INTERVAL=1

echo "PostgreSQL 준비 상태 확인 중..."

# Docker Compose로 컨테이너 시작 (이미 실행 중이면 무시)
docker compose up -d

for i in $(seq 1 $MAX_RETRIES); do
    if docker compose exec -T db pg_isready -U gift -d gift_test > /dev/null 2>&1; then
        echo "PostgreSQL 준비 완료!"
        exit 0
    fi
    echo "대기 중... ($i/$MAX_RETRIES)"
    sleep $RETRY_INTERVAL
done

echo "PostgreSQL 시작 실패"
exit 1
