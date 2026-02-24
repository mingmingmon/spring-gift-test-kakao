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
