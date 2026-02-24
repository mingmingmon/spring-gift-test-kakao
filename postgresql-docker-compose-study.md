# PostgreSQL + Docker Compose 학습 노트

## 1. Spring 프로파일로 DB를 분리하는 이유

- 개발용 DB와 테스트용 DB의 URL이 같으면 분리한 의미가 없음
- 하나의 PostgreSQL 컨테이너 안에 DB 2개(`gift_dev`, `gift_test`)를 만들어야 실제 분리가 됨
- `docker-compose.yml`의 `POSTGRES_DB`는 1개만 지정 가능하므로, 추가 DB는 `init-db.sh` 초기화 스크립트로 생성

| 프로파일 | 설정 파일 | DB | 용도 |
|---------|----------|-----|------|
| (기본) | `application.properties` | `gift_dev` | 로컬 개발 |
| test | `application-test.properties` | `gift_test` | 테스트 실행 |

## 2. H2와 PostgreSQL의 SQL 차이점

- H2의 `SET REFERENTIAL_INTEGRITY FALSE/TRUE`는 PostgreSQL에 없음
- PostgreSQL에서는 `TRUNCATE TABLE ... CASCADE`로 FK 제약 조건을 무시하며 삭제
- 테이블을 하나씩 TRUNCATE하지 않고 한 줄로 처리 가능

```sql
-- H2
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
SET REFERENTIAL_INTEGRITY TRUE;

-- PostgreSQL
TRUNCATE TABLE wish, option, product, category, member CASCADE;
```

## 3. PostgreSQL 예약어 충돌 문제

- `option`은 PostgreSQL의 예약어이므로 테이블 이름으로 그대로 사용하면 충돌 발생
- 해결 방법 2가지:
  1. SQL에서 큰따옴표로 감싸기: `TRUNCATE TABLE "option" CASCADE`
  2. 엔티티의 테이블명 자체를 변경: `@Table(name = "product_option")` — 더 깔끔
- 방법 1을 선택함

## 4. H2 전용 SQL 문법은 PostgreSQL에서 동작하지 않음

- `MERGE INTO`는 H2 전용 문법 → PostgreSQL에서는 `BadSqlGrammarException` 발생
- PostgreSQL에서는 `INSERT ... ON CONFLICT`를 사용해야 함

```sql
-- H2
MERGE INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com')

-- PostgreSQL
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com') ON CONFLICT (id) DO NOTHING
```

- `ON CONFLICT (id) DO NOTHING`: 이미 해당 id가 존재하면 무시 (MERGE INTO와 동일한 효과)

## 5. Gradle 버전별 API 차이

- `includeTestNames`는 Gradle 8.4에서 지원되지 않음
- `includeTest 'gift.cucumber.CucumberTest', '*'`로 대체해야 함

```groovy
// 오류
filter {
    includeTestNames 'gift.cucumber.CucumberTest'
}

// 정상
filter {
    includeTest 'gift.cucumber.CucumberTest', '*'
}
```

## 6. cucumberTest 실행 시 DB 자동 시작 흐름

`./gradlew cucumberTest` 한 줄로 DB 시작부터 테스트 실행까지 자동화되는 구조:

```
./gradlew cucumberTest
  │
  ├─ 1. startDb 태스크 실행 (dependsOn으로 연결)
  │     └─ scripts/wait-for-db.sh 실행
  │           ├─ docker compose up -d  → 컨테이너 시작 (이미 떠있으면 무시)
  │           └─ pg_isready 반복 체크  → PostgreSQL 준비 완료 대기
  │
  └─ 2. cucumberTest 태스크 실행 (DB 준비 완료 후)
        └─ gift.cucumber.CucumberTest 실행
              ├─ @Before: TRUNCATE CASCADE로 DB 초기화
              └─ 각 시나리오 실행
```

- `dependsOn 'startDb'`가 핵심: Gradle이 cucumberTest 전에 startDb를 먼저 실행하도록 보장
- DB가 이미 떠있으면 `docker compose up -d`가 바로 통과하고, 꺼져있으면 자동으로 시작

## 7. macOS에서 Docker 데몬 자동 시작

- macOS는 Linux가 아닌 **Unix(BSD) 기반**이라 Docker 데몬(`dockerd`)을 직접 실행할 수 없음
- Docker가 의존하는 cgroups, namespaces는 **Linux 커널 전용** 기능이므로 macOS에서는 반드시 **Linux VM**이 필요
- `docker` CLI는 클라이언트일 뿐, 데몬 없이는 동작하지 않음

### Docker 데몬을 띄워주는 도구들

| 도구 | 특징 |
|------|------|
| Docker Desktop | GUI 포함, 무거움 |
| colima | CLI만, 경량 VM (`brew install colima`) |
| OrbStack | 경량 대안 |

### wait-for-db.sh에서의 자동 시작 전략

Docker Desktop 실행 전에 먼저 colima로 경량 데몬 실행을 시도하고, colima가 없으면 Docker Desktop으로 폴백:

```bash
if ! docker info > /dev/null 2>&1; then
    if command -v colima > /dev/null 2>&1; then
        colima start           # 경량 VM만 시작
    else
        open -g -a Docker      # Docker Desktop 백그라운드 실행 (-g: 포커스 안 뺏김)
    fi
    # 데몬 준비될 때까지 대기...
fi
```

- `colima start`: Docker Desktop 없이 최소한의 Linux VM만 띄워서 Docker 데몬 실행
- `open -g -a Docker`: colima가 없을 때 Docker Desktop을 백그라운드로 실행 (폴백)
