#!/bin/bash
set -e

MAX_RETRIES=30
RETRY_INTERVAL=2
APP_URL="http://localhost:28080"

echo "앱 컨테이너 준비 상태 확인 중..."

for i in $(seq 1 $MAX_RETRIES); do
    if curl -sf "$APP_URL" > /dev/null 2>&1 || \
       curl -sf -o /dev/null -w "%{http_code}" "$APP_URL" 2>/dev/null | grep -q "^[2-4]"; then
        echo "앱 컨테이너 준비 완료!"
        exit 0
    fi
    echo "대기 중... ($i/$MAX_RETRIES)"
    sleep $RETRY_INTERVAL
done

echo "앱 컨테이너 시작 실패"
exit 1
