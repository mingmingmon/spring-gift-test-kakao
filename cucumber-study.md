# Cucumber BDD 학습 노트

## 1. .feature 파일과 Step 정의의 역할

### .feature 파일 — "무엇을" 테스트할지 (What)

- 비즈니스 요구사항을 자연어(한글)로 기술한 명세서
- 개발자가 아닌 사람(기획자, QA)도 읽을 수 있음
- 코드가 전혀 없음 — 순수한 시나리오 설명
- 이것만으로는 테스트가 실행되지 않음

```gherkin
만일 "음료" 이름으로 카테고리를 생성하면
그러면 응답 상태 코드는 200이다
```

### Step 정의 (Step Definitions) — "어떻게" 테스트할지 (How)

- `.feature`의 각 줄을 실제 Java 코드로 연결하는 구현체
- `.feature`의 텍스트와 어노테이션 문자열이 정확히 매칭되어야 함
- 실제 API 호출, DB 검증 등 테스트 로직이 여기에 있음

```java
@만일("{string} 이름으로 카테고리를 생성하면")
public void 이름으로_카테고리를_생성하면(String name) {
    RestAssured.given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", name))
        .post("/api/categories");
}
```

### 둘의 관계

- `.feature`는 명세, Step은 구현
- Cucumber가 런타임에 텍스트 매칭으로 둘을 연결하여 테스트를 실행
- `"음료"` 같은 값은 `{string}` 파라미터로 Step 메서드에 전달됨

## 2. Cucumber와 Spring Boot 통합이 필요한 이유

- `.feature` 파일은 자연어 명세일 뿐, 그 자체로는 테스트를 실행할 수 없음
- 실제 테스트를 실행하려면 Spring Boot 서버가 떠야 API 호출이 가능하고, Cucumber 엔진이 동작해야 `.feature`를 읽고 Step 정의와 매칭할 수 있음
- Step 2의 통합 설정이 이 둘을 연결하는 다리 역할
- 이게 없으면 Cucumber가 Spring 컨텍스트를 모르기 때문에 `@Autowired`도, `RestAssured`로 API 호출도 불가능

### 통합을 위해 만든 파일 3개

1. **`junit-platform.properties`** — feature 파일 위치, step 코드 위치 등을 Cucumber에 알려주는 설정
2. **`CucumberSpringConfig.java`** — `@CucumberContextConfiguration` + `@SpringBootTest`로 Cucumber가 Spring 컨텍스트를 띄우게 하고 RestAssured 포트 연결
3. **`CucumberTest.java`** — `@Suite` + `@IncludeEngines("cucumber")`로 JUnit Platform에서 Cucumber 엔진을 실행하는 진입점

## 3. Step Definitions와 SharedContext

### Step Definitions가 하는 일

- `.feature`의 각 한글 Step을 실제 Java 코드로 구현한 것
- 도메인별로 클래스를 분리: `CategoryStepDefs`, `ProductStepDefs`, `GiftStepDefs`
- 각 클래스에서 RestAssured로 API를 호출하고, 응답을 검증하는 로직을 담당

### SharedContext의 역할

- 여러 Step 정의 클래스 간에 응답(Response), 카테고리 ID 등 상태를 공유하기 위한 객체
- 예: `CategoryStepDefs`에서 저장한 카테고리 ID를 `ProductStepDefs`에서 사용

### 트러블슈팅: NoSuchBeanDefinitionException

- Step 정의 클래스들이 생성자에서 `SharedContext`를 주입받고 있었음
- `SharedContext`가 일반 클래스여서 Spring이 그 존재를 몰랐음 → 빈을 찾을 수 없다는 에러 발생
- `@Component`를 붙여서 Spring 빈으로 등록해주니 해결

## 4. 시나리오 간 데이터 격리 (DatabaseCleanUp)

### Step 3에서 테스트가 실패한 이유

- 각 시나리오가 같은 DB를 공유하는데, 이전 시나리오에서 생성한 데이터가 남아있었음
- 예: "카테고리를 생성하면 조회할 수 있다" 시나리오에서 "음료"를 생성한 뒤, 다음 시나리오 "여러 카테고리를 생성하면 모두 조회된다"가 실행될 때 이미 "음료"가 DB에 남아있음
- "3개가 존재한다"를 기대했는데 실제로는 4개 → `AssertionError` 발생

### DatabaseCleanUp이 해결하는 방법

- Cucumber의 `@Before(order = 0)` Hook을 사용하여 **매 시나리오 시작 전에** 모든 테이블을 TRUNCATE
- 이전 시나리오의 데이터가 완전히 제거된 깨끗한 상태에서 다음 시나리오가 시작됨
- 각 시나리오가 독립적으로 실행되므로 실행 순서에 상관없이 결과가 동일

### 동작 흐름

```
시나리오 A 시작
  → @Before: DB 전체 TRUNCATE (깨끗한 상태)
  → Given/When/Then 실행
시나리오 A 종료

시나리오 B 시작
  → @Before: DB 전체 TRUNCATE (A의 데이터 사라짐)
  → Given/When/Then 실행
시나리오 B 종료
```