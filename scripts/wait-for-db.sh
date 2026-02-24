#!/bin/bash
set -e

MAX_RETRIES=30
RETRY_INTERVAL=1

echo "PostgreSQL 준비 상태 확인 중..."

# Docker 데몬이 실행 중인지 확인하고, 아니면 자동 시작
if ! docker info > /dev/null 2>&1; then
    if command -v colima > /dev/null 2>&1; then
        echo "Docker 데몬이 실행 중이 아닙니다. colima를 시작합니다..."
        colima start
    else
        echo "Docker 데몬이 실행 중이 아닙니다. Docker Desktop을 시작합니다..."
        open -g -a Docker
    fi
    for i in $(seq 1 $MAX_RETRIES); do
        if docker info > /dev/null 2>&1; then
            echo "Docker 데몬 준비 완료!"
            break
        fi
        echo "Docker 데몬 대기 중... ($i/$MAX_RETRIES)"
        sleep $RETRY_INTERVAL
    done
    if ! docker info > /dev/null 2>&1; then
        echo "Docker 데몬 시작 실패"
        exit 1
    fi
fi

# Docker Compose로 DB 컨테이너만 시작 (이미 실행 중이면 무시)
docker compose up -d db

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
