# spring-gift-test

## 사전 요구사항

- Java 21
- Docker / Docker Compose

## 테스트 실행 방법

### Cucumber 테스트 (PostgreSQL 자동 시작)

Docker가 실행 중인 상태에서:

```bash
./gradlew cucumberTest
```

PostgreSQL이 자동으로 시작되고 테스트가 실행됩니다.

### 전체 테스트 실행 (기존 테스트 + Cucumber)

```bash
./gradlew test
```

### DB 수동 관리

```bash
# DB 시작
docker compose up -d

# DB 중지
docker compose down

# DB 중지 + 데이터 삭제
docker compose down -v
```

### HTML 리포트 확인

테스트 실행 후 Feature/시나리오/Step별 성공·실패 상태를 시각적으로 확인할 수 있다.

```bash
open build/reports/cucumber.html
```