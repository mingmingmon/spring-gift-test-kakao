# Application 컨테이너화 학습 기록

## 1. docker-compose.yml에서 named volume 선언 누락

**에러 메시지:**
```
service "db" refers to undefined volume gift-data: invalid compose project
```

**원인:** `db` 서비스에서 `gift-data:/var/lib/postgresql/data`로 named volume을 참조하지만, 파일 최하단에 최상위 `volumes:` 선언이 없었음.

**해결:** `docker-compose.yml` 끝에 추가:
```yaml
volumes:
  gift-data:
```

**배운 점:** Docker Compose에서 named volume을 사용하려면 서비스 내 `volumes:` 매핑뿐만 아니라 **최상위 `volumes:` 섹션에도 선언**해야 한다.

---

## 2. Dockerfile의 COPY 경로 오류

**에러 메시지:**
```
failed to calculate checksum of ref ... "/docs": not found
```

**원인:** `COPY . .` (프로젝트 전체 복사)이어야 하는데 `COPY docs .`로 작성되어 `docs` 디렉토리만 복사하려고 시도함.

**해결:** `COPY docs .` → `COPY . .`으로 수정.

**배운 점:** Multi-stage build에서 빌드 스테이지는 프로젝트 전체가 필요하므로 `COPY . .`으로 전체를 복사해야 한다. `.dockerignore`로 불필요한 파일을 제외하는 것이 올바른 접근.

---

## 3. Gradle dependsOn의 실행 순서

**혼동했던 점:** `dependsOn`을 보고 "이 태스크가 먼저 실행된다"고 착각.

**실제 동작:** `dependsOn`은 **"이 태스크를 실행하기 전에 먼저 실행해야 할 태스크"**를 지정하는 것. 의존성 체인을 거꾸로 따라가면 실제 실행 순서가 된다.

```groovy
cucumberTest  dependsOn 'waitForApp'
waitForApp    dependsOn 'dockerUp'
dockerUp      dependsOn 'startDb'
```

**실제 실행 순서:** `startDb` → `dockerUp` → `waitForApp` → `cucumberTest`

---

## 4. Gradle 태스크 캐싱 (UP-TO-DATE)

**현상:** `./gradlew cucumberTest`를 실행하면 테스트 출력 없이 `BUILD SUCCESSFUL`만 표시됨.

**원인:** Gradle은 입력(소스 코드, 설정 파일 등)과 출력이 이전 실행과 동일하면 태스크를 `UP-TO-DATE`로 판단하고 **스킵**한다.

**해결 방법 3가지:**
1. `./gradlew cucumberTest --rerun` — 캐시 무시하고 강제 재실행
2. `./gradlew clean cucumberTest` — 빌드 결과 삭제 후 재실행
3. `build.gradle`에 `outputs.upToDateWhen { false }` 추가 — 항상 재실행하도록 설정

```groovy
tasks.register('cucumberTest', Test) {
    outputs.upToDateWhen { false }  // 항상 실행
    // ...
}
```

**배운 점:** E2E 테스트처럼 외부 상태(Docker 컨테이너)에 의존하는 태스크는 Gradle 캐시가 의미 없으므로 `outputs.upToDateWhen { false }`를 설정하는 것이 적절하다.

---

## 5. Gradle `-x` 옵션으로 특정 태스크 제외

**상황:** 컨테이너가 이미 떠있는 상태에서 cucumberTest만 단독 실행하고 싶음.

**명령어:**
```bash
./gradlew cucumberTest -x waitForApp -x dockerUp -x startDb
```

**배운 점:** `-x <태스크명>`은 해당 태스크를 실행에서 제외(exclude)하는 옵션. 의존성 태스크를 건너뛰고 원하는 태스크만 실행할 때 유용하다.

---

## 6. Gradle 테스트 로그 출력 설정 (testLogging)

**현상:** 테스트가 실행되더라도 콘솔에 개별 테스트 결과(passed/failed)가 표시되지 않음.

**원인:** Gradle은 기본적으로 테스트 개별 결과를 콘솔에 출력하지 않음.

**해결:**
```groovy
tasks.register('cucumberTest', Test) {
    testLogging {
        events 'passed', 'failed', 'skipped'
        showStandardStreams = true
        exceptionFormat = 'full'
    }
}
```

**배운 점:**
- `events` — 콘솔에 표시할 이벤트 종류 (passed, failed, skipped, started 등)
- `showStandardStreams` — 테스트 코드의 stdout/stderr 출력 여부
- `exceptionFormat` — 실패 시 스택트레이스 출력 형식 (`short` 또는 `full`)

---

## 7. Docker Compose의 자동 네트워크 생성

**궁금했던 점:** `app` 컨테이너가 `db`에 서비스명으로 접근하는데, `networks:` 설정 없이 어떻게 통신이 되는가?

**동작 원리:** Docker Compose는 같은 `docker-compose.yml`에 정의된 서비스들을 **자동으로 같은 네트워크**에 넣어준다. 네트워크 이름은 `<프로젝트 디렉토리명>_default` 형식.

실제 로그에서 확인:
```
 Network spring-gift-test-kakao_default Creating
 Network spring-gift-test-kakao_default Created
```

이 덕분에 `app` 컨테이너에서 `jdbc:postgresql://db:5432/gift_test`처럼 서비스명 `db`를 호스트네임으로 사용할 수 있었음.

**명시적으로 `networks:`를 정의해야 하는 경우:**
- 여러 개의 docker-compose 파일 간에 서비스를 연결할 때
- 서비스를 서로 다른 네트워크로 격리하고 싶을 때 (예: frontend ↔ backend만, backend ↔ db만 통신)

---

## 8. webEnvironment = NONE과 Spring 컨텍스트의 차이

**궁금했던 점:** Docker로 앱을 띄우는데 왜 `webEnvironment = NONE`으로 설정하는가? 웹 서버와 Spring 컨텍스트는 무엇이 다른가?

**웹 서버 (내장 Tomcat):**
- Spring Boot가 내장하고 있는 Tomcat. `RANDOM_PORT`로 설정하면 테스트 JVM 안에서 Tomcat이 띄워져서 HTTP 요청을 받을 수 있음.

**Spring 컨텍스트:**
- Spring이 관리하는 Bean(객체)들의 집합. `@Service`, `@Repository`, `@Component` 등을 인스턴스로 만들고 `@Autowired`로 주입해주는 역할.

**`NONE`을 쓴 이유:**
- Docker 컨테이너 안에서 이미 내장 Tomcat이 8080 포트로 실행 중 (실제 테스트 대상)
- 테스트 JVM에서도 `RANDOM_PORT`로 Tomcat을 띄우면 **Tomcat이 두 개** 떠버림
- `NONE`으로 설정하면 Tomcat은 안 띄우되 Spring 컨텍스트는 유지 → `JdbcTemplate` 주입 가능

| | `RANDOM_PORT` (기존) | `NONE` (변경 후) |
|---|---|---|
| 내장 Tomcat (웹 서버) | 띄움 | 안 띄움 |
| Spring 컨텍스트 (Bean 관리) | 있음 | 있음 |
| `@Autowired JdbcTemplate` | 사용 가능 | 사용 가능 |

**결론:** `NONE`은 "웹 서버만 빼고 나머지 Spring 기능은 전부 쓰겠다"는 설정. 덕분에 기존 `DatabaseCleanUp`, `StepDefs` 코드를 수정 없이 그대로 사용할 수 있었음.

---

## 9. Docker 컨테이너와 테스트 JVM은 별개의 프로세스


```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│  Docker 컨테이너             │     │  내 Mac (호스트)              │
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

- **Docker 컨테이너**: `docker compose up`으로 띄운 앱. 컨테이너 안에서 JVM이 돌고 있음
- **테스트 JVM**: `./gradlew cucumberTest`를 실행하면 **Mac에서 직접** 새 JVM 프로세스가 시작됨. Docker 안이 아님

**테스트 JVM에서 Spring 컨텍스트를 띄우는 이유:**
- `DatabaseCleanUp`과 `StepDefs`에서 `@Autowired JdbcTemplate`으로 DB에 직접 접근해야 함
- Spring 컨텍스트 없이는 `JdbcTemplate` 객체를 자동 생성/주입받을 수 없음
- 수동으로 DataSource, JdbcTemplate을 설정하면 코드가 훨씬 복잡해짐

---

## 10. Gradle dependsOn으로 dockerBuild를 체인에 포함시키기

**문제:** `./gradlew cucumberTest`의 의존성 체인에 `dockerBuild`가 빠져있어서 이미지 빌드를 별도로 해야 했음.

**기존 체인:**
```
startDb → dockerUp → waitForApp → cucumberTest
```
`dockerBuild`가 체인에 없어서 코드 변경 후 `./gradlew dockerBuild`를 먼저 실행해야 했음.

**해결:** `dockerUp`의 `dependsOn`에 `dockerBuild`를 추가:
```groovy
tasks.register('dockerUp', Exec) {
    commandLine 'docker', 'compose', 'up', '-d'
    dependsOn 'startDb', 'dockerBuild'
}
```

**변경 후 체인:**
```
startDb ──┐
           ├→ dockerUp → waitForApp → cucumberTest → dockerDown
dockerBuild┘
```

`startDb`와 `dockerBuild`는 서로 의존성이 없으므로 Gradle이 병렬로 실행할 수 있음.

**결과:** `./gradlew cucumberTest` 한 줄로 이미지 빌드부터 테스트 실행, 컨테이너 정리까지 전부 자동화. (`finalizedBy`로 `dockerDown` 연결)

---

## 11. dockerBuild도 Docker 데몬이 필요하다

**에러 메시지:**
```
failed to connect to the docker API at unix:///...docker.sock;
check if the path is correct and if the daemon is running
```

**원인:** `dockerBuild`와 `startDb`가 서로 의존성이 없어서 Gradle이 `dockerBuild`를 먼저 실행할 수 있었음. Docker 데몬이 아직 안 켜진 상태에서 `docker compose build`를 시도하여 실패.

**해결:** `dockerBuild`도 `startDb`에 의존하도록 변경:
```groovy
tasks.register('dockerBuild', Exec) {
    commandLine 'docker', 'compose', 'build', 'app'
    dependsOn 'startDb'
}
```

**변경 후 체인:**
```
startDb → dockerBuild ─┐
                        ├→ dockerUp → waitForApp → cucumberTest → dockerDown
startDb ────────────────┘
```

**배운 점:** `dependsOn`이 없는 태스크들은 Gradle이 어떤 순서로든 실행할 수 있다. Docker 데몬이 필요한 태스크는 반드시 데몬 시작 태스크에 의존시켜야 한다.

---

## 12. dockerBuild를 매번 실행해도 괜찮은 이유 (Docker 레이어 캐싱)

**궁금했던 점:** `dockerBuild`를 체인에 포함시키면 매번 이미지를 다시 빌드해야 하는 것 아닌가? Multi-stage build의 이점을 못 누리게 되는 것 아닌가?

**답: 아니다. Docker의 레이어 캐싱 덕분에 코드가 변경되지 않으면 몇 초 만에 끝난다.**

`docker compose build`는 매번 실행되지만, Dockerfile의 각 스텝(레이어)을 이전 결과와 비교하여 변경이 없으면 `CACHED`로 처리한다.

코드 변경 없이 빌드할 때:
```
#7 [build 2/4] WORKDIR /app
#7 CACHED

#8 [build 3/4] COPY . .
#8 CACHED

#9 [build 4/4] RUN gradle bootJar --no-daemon
#9 CACHED
```

코드를 수정했을 때만 `COPY . .` 이후 스텝이 다시 실행됨.

**결론:** Multi-stage build의 캐싱 이점은 그대로 유지되므로, 매번 체인에 포함시켜도 실질적인 성능 손해가 거의 없다.

---

## 13. `./gradlew test`에서 Cucumber 테스트 완전 제외하기

**문제:** `./gradlew test` 실행 시 Cucumber 테스트까지 함께 실행되어 실패함. Cucumber 테스트는 Docker 앱 컨테이너가 필요한데 `test` 태스크는 DB만 띄우기 때문.

**시도 1: `excludeTest` (실패)**
```groovy
filter {
    excludeTest 'gift.cucumber.*', '*'
}
```
Gradle의 `excludeTest`는 JUnit 테스트 클래스 이름 기반 필터. Cucumber 엔진은 `.feature` 파일에서 직접 테스트를 발견하기 때문에 이 필터를 우회함.

**시도 2: `exclude` 클래스 파일 (부분 성공)**
```groovy
exclude 'gift/cucumber/**'
```
`CucumberTest.class` 등 클래스 파일을 제외하여 Suite 엔진을 통한 발견은 막음. 하지만 Cucumber 엔진이 `.feature` 파일을 직접 스캔해서 여전히 21개 시나리오가 실행됨.

**시도 3: `excludeEngines` (부분 성공)**
```groovy
useJUnitPlatform {
    excludeEngines 'cucumber'
}
```
Cucumber 엔진의 직접 발견은 막음. 하지만 `CucumberTest`에 붙은 `@Suite`가 JUnit Suite 엔진을 통해 Cucumber를 다시 호출함.

**최종 해결: 둘 다 적용 (성공)**
```groovy
tasks.named('test') {
    useJUnitPlatform {
        excludeEngines 'cucumber'    // Cucumber 엔진의 직접 발견 차단
    }
    exclude 'gift/cucumber/**'       // Suite 엔진을 통한 간접 발견 차단
    dependsOn 'startDb'
}
```

**배운 점:** Cucumber 테스트는 두 가지 경로로 발견될 수 있다:
1. Cucumber 엔진이 `.feature` 파일을 직접 스캔
2. `@Suite` 어노테이션이 붙은 `CucumberTest` 클래스를 JUnit Suite 엔진이 발견

두 경로를 모두 막아야 완전히 제외된다.

---

## 14. `wait-for-db.sh`에서 DB 컨테이너만 시작하기

**문제:** `wait-for-db.sh`가 `docker compose up -d`로 **모든 서비스**(DB + App)를 시작함. `./gradlew test`는 DB만 필요한데 앱 컨테이너까지 뜨면서 같은 `gift_test` DB를 `create-drop`으로 점유 → 테스트 JVM과 스키마 충돌.

**해결:** `docker compose up -d` → `docker compose up -d db`로 변경하여 DB 컨테이너만 시작.

**배운 점:** `docker compose up -d`는 `docker-compose.yml`에 정의된 **모든 서비스**를 시작한다. 특정 서비스만 시작하려면 서비스명을 명시해야 한다 (`docker compose up -d db`).
