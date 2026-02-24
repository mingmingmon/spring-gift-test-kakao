# spring-gift-test

## 사전 요구사항

- Java 21
- Docker / Docker Compose

## 테스트 명령어 비교

### `./gradlew test` — 인수 테스트

```
┌─────────────────────────────────────┐
│  테스트 JVM (내 Mac)                  │
│                                     │
│  Spring Boot 앱 (내장 Tomcat)        │
│  - RANDOM_PORT로 실행               │
│  - RestAssured → 내장 Tomcat에 요청  │
│  - JdbcTemplate → DB 접근            │
└──────────────────┬──────────────────┘
                   │
          ┌────────┴───────┐
          │  Docker DB     │
          │  PostgreSQL    │
          │  포트 5432     │
          └────────────────┘
```

- 테스트 JVM 안에서 **내장 Tomcat을 직접 띄움**
- `BaseAcceptanceTest` 기반 테스트만 실행 (Cucumber 제외)
- Docker는 **DB만** 띄움 (앱 컨테이너 없음)
- 실행 흐름: `startDb → test`

### `./gradlew cucumberTest` — Cucumber E2E 테스트

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│  Docker 컨테이너             │     │  테스트 JVM (내 Mac)          │
│                             │     │                             │
│  Spring Boot 앱 (Tomcat)    │     │  Tomcat 안 띄움 (NONE)       │
│  - API 요청 처리             │     │  - RestAssured → Docker 앱  │
│  - 포트 8080 (→ 28080 매핑)  │     │  - JdbcTemplate → DB 접근    │
└──────────┬──────────────────┘     └──────────┬──────────────────┘
           │                                   │
           │         ┌──────────────┐          │
           └────────▶│  Docker DB   │◀─────────┘
                     │  PostgreSQL  │
                     │  포트 5432   │
                     └──────────────┘
```

- 앱을 **Docker 컨테이너로** 띄움, 테스트 JVM은 웹 서버를 띄우지 않음
- Cucumber `.feature` 파일 기반 시나리오 실행
- Docker로 **DB + App** 둘 다 띄움
- 실행 흐름: `startDb → dockerBuild → dockerUp → waitForApp → cucumberTest → dockerDown`

### 요약 표

| | `./gradlew test` | `./gradlew cucumberTest` |
|---|---|---|
| 앱 실행 위치 | 테스트 JVM (내장 Tomcat) | Docker 컨테이너 |
| DB 실행 위치 | Docker | Docker |
| 웹 서버 | `RANDOM_PORT` | `NONE` (안 띄움) |
| 테스트 대상 | `BaseAcceptanceTest` 기반 | Cucumber `.feature` 시나리오 |
| RestAssured 대상 | `localhost:랜덤포트` | `localhost:28080` |
| 컨테이너 정리 | 안 함 (DB 유지) | 자동 (`dockerDown`) |

## 테스트 실행 방법

### 1. 인수 테스트 실행

```bash
./gradlew test
```

DB만 자동으로 시작됩니다.

### 2. Cucumber E2E 테스트 실행

```bash
./gradlew cucumberTest
```

이미지 빌드 → 컨테이너 기동 → 테스트 실행 → 컨테이너 정리까지 전부 자동으로 실행됩니다.

> 테스트 성공/실패와 관계없이 `dockerDown`이 실행되어 컨테이너가 정리됩니다. (`finalizedBy` 사용)

### 3. 테스트 결과 확인

```bash
# Cucumber 결과 요약 보기
./gradlew cucumberTestReport

# HTML 리포트 열기
open build/reports/tests/cucumberTest/index.html  # Cucumber
open build/reports/tests/test/index.html           # 인수 테스트
```
