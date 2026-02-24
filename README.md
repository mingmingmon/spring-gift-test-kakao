# spring-gift-test

## 사전 요구사항

- Java 21
- Docker / Docker Compose

## 시스템 구조

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│  Docker 컨테이너             │     │  호스트 (내 Mac)              │
│                             │     │                             │
│  Spring Boot 앱 (Tomcat)    │     │  ./gradlew cucumberTest     │
│  - API 요청 처리             │     │  = 테스트 JVM               │
│  - 포트 8080 (→ 28080 매핑)  │     │  - RestAssured → HTTP 요청  │
│                             │     │  - JdbcTemplate → DB 접근    │
└──────────┬──────────────────┘     └──────────┬──────────────────┘
           │                                   │
           │         ┌──────────────┐          │
           └────────▶│  Docker DB   │◀─────────┘
                     │  PostgreSQL  │
                     │  포트 5432   │
                     └──────────────┘
```

- **Docker 컨테이너 앱**: 실제 테스트 대상. HTTP API를 처리하는 Spring Boot 앱
- **테스트 JVM**: Mac에서 Gradle이 실행하는 프로세스. RestAssured로 Docker 앱에 HTTP 요청을 보내고, JdbcTemplate으로 DB에 직접 접근하여 데이터 세팅/클린업/검증 수행
- **Docker DB**: PostgreSQL. 앱과 테스트 JVM 양쪽에서 접근

## 테스트 실행 방법

### 1. Cucumber E2E 테스트 실행

```bash
./gradlew cucumberTest
```

이 명령어 하나로 이미지 빌드 → 컨테이너 기동 → 테스트 실행 → 컨테이너 정리까지 전부 자동으로 실행됩니다:

```
startDb (Docker 데몬 확인 + DB 시작) → dockerBuild (앱 이미지 빌드) ─┐
                                                                     ├→ dockerUp → waitForApp → cucumberTest → dockerDown
startDb ─────────────────────────────────────────────────────────────┘
```

> 테스트 성공/실패와 관계없이 `dockerDown`이 실행되어 컨테이너가 정리됩니다. (`finalizedBy` 사용)

### 2. 테스트 결과 확인

```bash
# 콘솔에서 결과 요약 보기
./gradlew cucumberTestReport

# HTML 리포트 열기
open build/reports/tests/cucumberTest/index.html
```

### 3. 전체 테스트 실행 (기존 단위/인수 테스트)

```bash
./gradlew test
```
