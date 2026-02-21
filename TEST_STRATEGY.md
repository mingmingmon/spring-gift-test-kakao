# TEST_STRATEGY.md

## 1. 검증할 행동 목록 및 선정 기준

### 선정 기준

CLAUDE.md에 정의된 다음 기준을 적용하여 핵심 행동을 선정했다:

- 상태 변화가 발생하는 기능
- 비즈니스 리스크가 큰 기능
- 실패 시나리오가 명확한 기능
- 리팩터링 시 깨질 가능성이 높은 경계 기능

### 핵심 행동 5개

| # | 행동 | 유형 | 상태 변화 | 선정 이유 |
|---|------|------|----------|----------|
| 1 | 카테고리 생성 | 성공 | Category 레코드 생성 | 상품 생성의 전제 조건. 데이터 무결성의 출발점 |
| 2 | 상품 생성 | 성공 | Product 레코드 생성 | Category 참조 필수. 잘못된 참조 시 실패해야 하는 경계 조건 존재 |
| 3 | 상품 조회 | 검증 수단 | 없음 (읽기) | 생성 행동의 결과를 후속 API로 확인하는 검증 수단 |
| 4 | 선물하기 (재고 차감) | 성공 | Option.quantity 감소 | 핵심 비즈니스 로직. 재고 차감 + 외부 배송이 트랜잭션으로 묶여 리스크 최대 |
| 5 | 선물하기 실패 (재고 부족) | 실패 | 상태 불변 | 재고 부족 시 요청 거부 + 재고 미변경. 트랜잭션 롤백의 간접 증명 |

---

## 2. 시나리오 설계 (CLAUDE.md Step 2)

각 도메인별로 성공 시나리오와 실패 시나리오를 나누어 설계한다.
실패 시나리오는 입력값 문제뿐 아니라, 시스템 상태에 따라 발생할 수 있는 예외 상황을 포함한다.

---

### 2.1 카테고리 도메인

#### 성공 시나리오

**S-CAT-1. 카테고리 생성 후 조회**
```
Given: 시스템이 초기 상태이다
When:  POST /api/categories 로 카테고리를 생성한다
Then:  응답에 생성된 카테고리 정보(id, name)가 포함된다
And:   GET /api/categories 로 조회하면 생성한 카테고리가 존재한다
```

**S-CAT-2. 여러 카테고리 생성 후 전체 조회**
```
Given: 시스템이 초기 상태이다
When:  POST /api/categories 로 카테고리 A, B, C를 생성한다
Then:  GET /api/categories 로 조회하면 3개의 카테고리가 모두 존재한다
```

#### 실패 시나리오

**F-CAT-1. 이름이 null인 카테고리 생성**
```
Given: 시스템이 초기 상태이다
When:  POST /api/categories 로 name = null 인 카테고리를 생성한다
Then:  요청이 실패하거나, 비정상적인 데이터가 저장된다
And:   GET /api/categories 로 조회하여 실제 상태를 확인한다
```

> 현재 코드에 입력 검증이 없으므로, name=null인 카테고리가 그대로 저장될 가능성이 높다.
> 이 테스트는 현재 시스템의 실제 행동을 기록하는 목적으로 작성한다.

---

### 2.2 상품 도메인

#### 성공 시나리오

**S-PRD-1. 상품 생성 후 조회**
```
Given: 카테고리가 존재한다 (API로 생성)
When:  POST /api/products 로 상품을 생성한다 (name, price, imageUrl, categoryId)
Then:  응답에 생성된 상품 정보가 포함된다
And:   GET /api/products 로 조회하면 생성한 상품이 존재한다
```

**S-PRD-2. 서로 다른 카테고리에 상품 각각 생성**
```
Given: 카테고리 A, B가 존재한다
When:  카테고리 A에 상품 1을, 카테고리 B에 상품 2를 생성한다
Then:  GET /api/products 로 조회하면 2개의 상품이 존재한다
And:   각 상품의 카테고리가 올바르게 연결되어 있다
```

#### 실패 시나리오

**F-PRD-1. 존재하지 않는 카테고리로 상품 생성**
```
Given: categoryId = 999999 (존재하지 않는 카테고리)
When:  POST /api/products 로 상품 생성을 요청한다
Then:  요청이 실패한다 (에러 응답)
And:   GET /api/products 로 조회하면 상품이 생성되지 않았다 (상태 불변)
```

**F-PRD-2. categoryId가 null인 상품 생성**
```
Given: 시스템이 초기 상태이다
When:  POST /api/products 로 categoryId = null 인 상품을 생성한다
Then:  요청이 실패한다
And:   GET /api/products 로 조회하면 상품이 생성되지 않았다 (상태 불변)
```

> `categoryRepository.findById(null)` → IllegalArgumentException 예상

**F-PRD-3. 가격이 음수인 상품 생성**
```
Given: 카테고리가 존재한다
When:  POST /api/products 로 price = -1000 인 상품을 생성한다
Then:  요청이 실패하거나, 음수 가격 상품이 저장된다
And:   GET /api/products 로 조회하여 실제 상태를 확인한다
```

> 입력 검증이 없으므로 음수 가격이 그대로 저장될 가능성이 높다. 결함 가능성.

**F-PRD-4. 가격이 0인 상품 생성**
```
Given: 카테고리가 존재한다
When:  POST /api/products 로 price = 0 인 상품을 생성한다
Then:  요청이 실패하거나, 가격 0 상품이 저장된다
And:   GET /api/products 로 조회하여 실제 상태를 확인한다
```

> 비즈니스 규칙에 따라 0원 상품이 허용되는지 판단이 필요한 경계값.

---

### 2.3 선물하기 도메인

#### 성공 시나리오

**S-GIFT-1. 선물하기 성공 시 재고 차감**
```
Given: 보내는 회원, 받는 회원, 상품, 옵션(재고 10개)이 존재한다
When:  POST /api/gifts 로 선물하기를 요청한다 (optionId, quantity=3, receiverId, message)
       Header: Member-Id = {보내는 회원 ID}
Then:  요청이 성공한다 (200)
And:   옵션의 재고가 10 → 7로 감소했다
```

**S-GIFT-2. 연속 선물로 재고 누적 차감**
```
Given: 옵션 재고가 10개이다
When:  3개를 선물한다
Then:  재고가 7이 된다
When:  4개를 선물한다
Then:  재고가 3이 된다
```

> 여러 번의 선물이 독립적으로 정확히 차감되는지 확인한다.

**S-GIFT-3. 재고 전부 소진 (경계값)**
```
Given: 옵션 재고가 3개이다
When:  정확히 3개를 선물한다
Then:  요청이 성공한다 (200)
And:   재고가 정확히 0이 된다
```

#### 실패 시나리오 - 재고 관련

**F-GIFT-1. 재고 부족으로 선물 실패 + 상태 불변**
```
Given: 옵션 재고가 5개이다
When:  POST /api/gifts 로 quantity = 10 인 선물하기를 요청한다
Then:  요청이 실패한다 (에러 응답)
And:   옵션의 재고가 여전히 5개이다 (상태 불변)
```

> 트랜잭션 롤백의 간접 증명.

**F-GIFT-2. 재고 소진 후 추가 선물 실패**
```
Given: 옵션 재고가 3개이다
When:  3개를 선물한다 (성공)
And:   다시 1개를 선물하려 한다
Then:  재고 부족으로 실패한다
And:   재고는 여전히 0이다
```

**F-GIFT-3. 수량이 0인 선물하기**
```
Given: 옵션 재고가 10개이다
When:  POST /api/gifts 로 quantity = 0 인 선물하기를 요청한다
Then:  요청의 성공/실패 여부를 확인한다
And:   재고가 변하지 않았는지 확인한다
```

> `Option.decrease(0)`: 조건 `10 < 0`은 false → 예외 없이 통과.
> 수량 0의 선물이 성공하는 것이 현재 시스템의 실제 행동이다. 결함 가능성.

**F-GIFT-4. 수량이 음수인 선물하기 (버그 검증)**
```
Given: 옵션 재고가 10개이다
When:  POST /api/gifts 로 quantity = -5 인 선물하기를 요청한다
Then:  요청의 성공/실패 여부를 확인한다
And:   재고 상태를 확인한다
```

> **명확한 버그**: `Option.decrease(-5)` → 조건 `10 < -5`는 false → 예외 없이 통과
> → `this.quantity -= (-5)` → 재고가 10 → 15로 **증가**한다.
> 이 테스트는 이 버그를 가시화하는 역할을 한다.

#### 실패 시나리오 - 존재하지 않는 리소스

**F-GIFT-5. 존재하지 않는 옵션으로 선물하기**
```
Given: 회원이 존재한다
When:  POST /api/gifts 로 optionId = 999999 (존재하지 않음) 인 선물하기를 요청한다
Then:  요청이 실패한다 (에러 응답)
```

> `optionRepository.findById(999999).orElseThrow()` → NoSuchElementException → 500 예상

**F-GIFT-6. 존재하지 않는 보내는 회원으로 선물하기 (트랜잭션 롤백 검증)**
```
Given: 옵션(재고 10개)이 존재한다
When:  POST /api/gifts 를 Member-Id = 999999 (존재하지 않는 회원) 로 요청한다
Then:  요청이 실패한다 (에러 응답)
And:   옵션의 재고가 여전히 10개이다 (상태 불변)
```

> GiftService에서 memberId는 검증 없이 Gift 객체에 전달된다.
> FakeGiftDelivery.deliver()에서 `memberRepository.findById(gift.getFrom()).orElseThrow()`가 실패한다.
> **핵심**: `option.decrease()`가 먼저 실행된 후 deliver()에서 실패하므로,
> 트랜잭션 롤백이 정상 작동하여 재고가 원복되는지를 검증할 수 있다.

**F-GIFT-7. 존재하지 않는 받는 회원에게 선물하기**
```
Given: 보내는 회원, 옵션(재고 10개)이 존재한다
When:  POST /api/gifts 로 receiverId = 999999 (존재하지 않는 회원) 인 선물하기를 요청한다
Then:  요청의 성공/실패 여부를 확인한다
And:   재고 상태를 확인한다
```

> FakeGiftDelivery.deliver()는 `gift.getFrom()`(보내는 사람)만 조회하고
> `gift.getTo()`(받는 사람)은 조회하지 않는다.
> 따라서 존재하지 않는 수신자에게 선물이 **성공**할 가능성이 높다. 결함 가능성.

#### 실패 시나리오 - 요청 형식 오류

**F-GIFT-8. Member-Id 헤더 없이 선물하기**
```
Given: 옵션이 존재한다
When:  POST /api/gifts 를 Member-Id 헤더 없이 요청한다
Then:  요청이 실패한다 (에러 응답)
And:   재고가 변하지 않았다 (상태 불변)
```

> `@RequestHeader("Member-Id")`가 누락되면 Spring이 400 Bad Request를 반환할 것으로 예상.

**F-GIFT-9. 요청 바디 없이 선물하기**
```
Given: 회원이 존재한다
When:  POST /api/gifts 를 body 없이, Member-Id 헤더만 포함하여 요청한다
Then:  요청이 실패한다 (에러 응답)
And:   재고가 변하지 않았다 (상태 불변)
```

> `@RequestBody`가 있으므로 body가 없으면 Spring이 400 Bad Request를 반환할 것으로 예상.

---

### 시나리오 요약

#### 성공 시나리오

| ID | 도메인 | 시나리오 | 검증 포인트 |
|----|--------|---------|------------|
| S-CAT-1 | 카테고리 | 생성 후 조회 | 응답 200 + 후속 조회 존재 확인 |
| S-CAT-2 | 카테고리 | 여러 개 생성 후 전체 조회 | 목록 개수 + 내용 일치 |
| S-PRD-1 | 상품 | 생성 후 조회 | 응답 200 + 후속 조회 존재 확인 |
| S-PRD-2 | 상품 | 다른 카테고리에 각각 생성 | 카테고리 연결 정확성 |
| S-GIFT-1 | 선물 | 선물 성공 시 재고 차감 | 재고 감소 확인 |
| S-GIFT-2 | 선물 | 연속 선물 누적 차감 | 누적 차감 정확성 |
| S-GIFT-3 | 선물 | 재고 전부 소진 (경계값) | 재고 정확히 0 |

#### 실패 시나리오

| ID | 도메인 | 시나리오 | 예상 현재 행동 | 비즈니스 판단 |
|----|--------|---------|---------------|-------------|
| F-CAT-1 | 카테고리 | name=null | 저장됨 (검증 없음) | 결함 가능성 |
| F-PRD-1 | 상품 | 존재하지 않는 카테고리 | 실패 (500) | 정상 (거부됨) |
| F-PRD-2 | 상품 | categoryId=null | 실패 (예외) | 정상 (거부됨) |
| F-PRD-3 | 상품 | price 음수 | 저장됨 (검증 없음) | 결함 가능성 |
| F-PRD-4 | 상품 | price=0 | 저장됨 | 판단 필요 |
| F-GIFT-1 | 선물 | 재고 부족 | 실패 + 상태 불변 | 정상 (거부됨) |
| F-GIFT-2 | 선물 | 재고 소진 후 추가 선물 | 실패 + 상태 불변 | 정상 (거부됨) |
| F-GIFT-3 | 선물 | quantity=0 | 성공 (재고 불변) | 결함 가능성 |
| F-GIFT-4 | 선물 | quantity 음수 | 성공 (재고 증가) | **명확한 버그** |
| F-GIFT-5 | 선물 | 존재하지 않는 옵션 | 실패 (500) | 정상 (거부됨) |
| F-GIFT-6 | 선물 | 존재하지 않는 보내는 회원 | 실패 + 롤백 | 트랜잭션 검증 포인트 |
| F-GIFT-7 | 선물 | 존재하지 않는 받는 회원 | 성공 (검증 안 함) | 결함 가능성 |
| F-GIFT-8 | 선물 | Member-Id 헤더 누락 | 실패 (400) | 정상 (거부됨) |
| F-GIFT-9 | 선물 | 요청 바디 없음 | 실패 (400) | 정상 (거부됨) |

> 이 시나리오들은 "현재 시스템이 어떻게 행동하는가"를 기록하는 것이 목적이다.
> 결함으로 판단되는 항목은 테스트가 현재 행동을 고정한 뒤, 별도로 수정을 논의한다.

---

## 3. 테스트 데이터 전략 (CLAUDE.md Step 3)

### 3.1 데이터 준비 우선순위

CLAUDE.md가 정의한 우선순위:
1. **API를 통한 데이터 준비** (최우선)
2. **테스트 전용 seed/fixture**
3. **DB 직접 조회** (최소화)

### 3.2 API로 준비 가능한 데이터

| 데이터 | 준비 방법 | 비고 |
|--------|----------|------|
| Category | `POST /api/categories` | API 존재 |
| Product | `POST /api/products` | API 존재. Category 선행 필요 |

### 3.3 API가 없어 대안이 필요한 데이터

| 데이터 | 문제 | 대안 |
|--------|------|------|
| Member | Service, Controller 모두 없음 | Repository 직접 사용 또는 TestFixture |
| Option | Controller 없음 (Service는 존재) | Repository 직접 사용 또는 TestFixture |

### 3.4 테스트 데이터 격리 전략

#### 전제 조건: `@Transactional` 롤백이 불가능한 이유

`@SpringBootTest(webEnvironment = RANDOM_PORT)`를 사용하면 실제 내장 서버가 기동된다.
RestAssured로 보내는 HTTP 요청은 **서버 스레드**에서 처리되고, 테스트 메서드는 **테스트 스레드**에서 실행된다.
두 스레드의 트랜잭션은 별개이므로, 테스트에 `@Transactional`을 붙여도 서버 측 데이터 변경은 롤백되지 않는다.

따라서 별도의 격리 전략이 필요하다.

#### 후보 전략 비교

| 전략 | 격리 보장 | 속도 | 유지보수 | 비고 |
|------|----------|------|---------|------|
| A. `@DirtiesContext` | 완벽 (Context + DB 재생성) | **매우 느림** | 코드 없음 | 테스트 증가 시 누적 비용 급증 |
| B. `@BeforeEach` + Repository `deleteAll()` | 보장됨 | 빠름 | FK 삭제 순서 수동 관리 | 엔티티 추가 시 순서 실수 위험 |
| C. `@BeforeEach` + SQL TRUNCATE (DatabaseCleaner) | **완벽** | **빠름** | 자동화 가능 | H2의 `SET REFERENTIAL_INTEGRITY FALSE` 활용 |
| D. 테스트마다 고유 데이터 (cleanup 없음) | 불완전 | 가장 빠름 | 검증 복잡 | GET 전체 조회 시 다른 테스트 데이터 섞임 |

#### 선택: 전략 C - `@BeforeEach` + SQL TRUNCATE

**선택 이유:**

1. **FK 순서를 신경 쓸 필요 없다**: `SET REFERENTIAL_INTEGRITY FALSE`로 순서 무관하게 모든 테이블을 비울 수 있다
2. **Context 재사용으로 빠르다**: `@DirtiesContext`의 느린 Context 재생성을 피한다
3. **조회 검증이 단순해진다**: 매 테스트가 빈 DB에서 시작하므로 "생성 후 조회하면 1개"처럼 명확한 검증이 가능하다

**탈락 이유:**

- 전략 A (`@DirtiesContext`): 현재 6개 테스트에서는 감당 가능하나, 테스트가 늘어나면 수 분 단위로 느려진다. 확장성 없음
- 전략 B (Repository `deleteAll()`): Option → Product → Category 순서처럼 FK 의존 순서를 수동으로 관리해야 한다. 엔티티가 추가되면 삭제 순서 실수로 테스트가 깨질 위험
- 전략 D (고유 데이터): `GET /api/products`가 전체 목록을 반환하므로, 이전 테스트의 데이터가 남아 있으면 "상품이 1개 존재한다" 같은 단순한 검증이 불가능해진다

#### 구현 방향: test 패키지 내 BaseAcceptanceTest

main 코드를 수정하지 않고, test 패키지 내부에서만 격리를 해결한다.

**제약 조건:**
- `@Component`로 main에 테스트 전용 코드를 넣지 않는다
- JPA 메타모델 자동화는 현재 엔티티 5개 규모에서 오버엔지니어링이므로 사용하지 않는다

**방식: BaseAcceptanceTest 부모 클래스에 `@BeforeEach` TRUNCATE 정의**

```java
// src/test/java/gift/BaseAcceptanceTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE wish");
        jdbcTemplate.execute("TRUNCATE TABLE option");
        jdbcTemplate.execute("TRUNCATE TABLE product");
        jdbcTemplate.execute("TRUNCATE TABLE category");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
```

**각 테스트 클래스에서의 사용:**
```java
// src/test/java/gift/CategoryAcceptanceTest.java
class CategoryAcceptanceTest extends BaseAcceptanceTest {
    // @BeforeEach 상속으로 매 테스트마다 DB 초기화
    // 테스트 메서드만 작성하면 됨
}
```

**이 방식의 장점:**
- main 패키지 수정 없음
- 테이블명 5개를 한 곳(BaseAcceptanceTest)에서만 관리
- 3개 테스트 클래스가 상속받으므로 중복 없음
- `RestAssured.port` 설정도 부모에서 한 번만 처리

### 3.5 Gift 테스트를 위한 데이터 셋업 방식

Gift API 테스트는 Member와 Option이 필수이지만 API가 없으므로, 다음 방식을 사용한다:

```
방식: @Autowired Repository를 통한 직접 저장

이유:
- CLAUDE.md 우선순위 2에 해당 (테스트 전용 fixture)
- API가 존재하지 않는 데이터에 한해서만 사용
- Category, Product는 API로 생성하고, Member/Option만 Repository로 준비
```

이는 "DB 직접 조회"가 아닌 "테스트 전용 fixture"에 해당하며,
API가 없는 데이터를 준비하기 위한 불가피한 선택이다.

---

## 4. 검증 전략

### 4.1 검증 계층

모든 검증은 API 경계에서 수행한다 (CLAUDE.md 3.2 원칙):

```
┌──────────────────────────────────────────┐
│  1차 검증: HTTP 응답                      │
│  - 상태 코드 (200, 4xx, 5xx)             │
│  - 응답 바디 (생성된 리소스 정보)          │
├──────────────────────────────────────────┤
│  2차 검증: 후속 API 호출로 상태 변화 확인  │
│  - GET 조회로 생성/변경 확인              │
│  - 실패 시 GET 조회로 상태 불변 확인       │
└──────────────────────────────────────────┘
```

### 4.2 검증하지 않는 것

CLAUDE.md 절대 원칙에 따라 다음은 검증하지 않는다:

- mock 객체 verify (GiftDelivery.deliver() 호출 여부 등)
- 내부 메서드 호출 여부
- 특정 클래스 구조나 의존 관계

### 4.3 Gift 재고 검증의 특수성

현재 Option 조회 API가 없으므로, 재고 변화 검증에 한해 다음 전략을 사용한다:

**전략 A: 반복 선물로 간접 검증**
```
재고 10개 → 7개 선물(성공) → 3개 선물(성공) → 1개 선물(실패)
= 정확히 10개가 차감되었음을 간접 증명
```

**전략 B: Repository 직접 조회 (보조)**
```
Option 조회 API가 없으므로, 재고 수량의 정확한 값 검증이 필요할 때
@Autowired OptionRepository로 직접 조회
```

전략 A를 우선 사용하되, 정확한 수량 확인이 필요한 경우 전략 B를 보조적으로 사용한다.

---

## 5. 주요 의사결정

### 5.1 테스트 도구 선택

| 항목 | 선택 | 이유 |
|------|------|------|
| 테스트 프레임워크 | RestAssured + Spring Boot Test | CLAUDE.md 지정 (Step 4) |
| BDD 도구 | 사용하지 않음 | CLAUDE.md 금지 사항 |
| Mock 프레임워크 | 사용하지 않음 | CLAUDE.md 금지 사항 |

### 5.2 `@RequestBody` 누락 문제 대응

project-analysis.md에서 식별한 `@RequestBody` 누락 문제(5.1):

- CategoryRestController와 ProductRestController의 POST 메서드에 `@RequestBody`가 없음
- **테스트는 현재 시스템의 실제 행동을 기록한다**
- `@RequestBody` 없이 동작하는 현재 바인딩 방식(form parameter)에 맞춰 테스트를 작성
- 만약 JSON body로 요청해야 하는 것이 원래 의도였다면, 이는 테스트가 아닌 코드 수정으로 해결해야 할 문제

### 5.3 에러 응답 코드 검증 범위

- 글로벌 예외 핸들러가 없어 대부분의 에러가 500으로 응답됨
- 테스트는 "요청이 실패한다"를 검증하되, 구체적인 상태 코드(400 vs 404 vs 500)에 과도하게 의존하지 않음
- 상태 코드보다 **실패 후 상태 불변**을 더 중요하게 검증

### 5.4 Wish 테스트 제외 결정

- Wish는 Controller가 없어 API 경계 테스트 자체가 불가능
- CLAUDE.md 원칙(3.2)에 따라 API 경계에서 시작해야 하므로, Wish 행동 테스트는 제외
- Controller가 구현되면 그때 추가

---

## 6. 테스트 클래스 구조 (예정)

```
src/test/java/gift/
├── BaseAcceptanceTest.java             # 공통 부모 (DB 초기화, RestAssured 설정)
├── CategoryAcceptanceTest.java         # 카테고리 도메인
├── ProductAcceptanceTest.java          # 상품 도메인
└── GiftAcceptanceTest.java             # 선물하기 도메인
```

### 테스트 메서드 매핑

**CategoryAcceptanceTest** (시나리오 2개)

| 메서드명 | 시나리오 ID | 유형 |
|---------|-----------|------|
| `카테고리를_생성하면_조회할_수_있다()` | S-CAT-1 | 성공 |
| `여러_카테고리를_생성하면_모두_조회된다()` | S-CAT-2 | 성공 |
| `이름이_null인_카테고리_생성_시_현재_행동을_확인한다()` | F-CAT-1 | 실패 |

**ProductAcceptanceTest** (시나리오 6개)

| 메서드명 | 시나리오 ID | 유형 |
|---------|-----------|------|
| `상품을_생성하면_조회할_수_있다()` | S-PRD-1 | 성공 |
| `서로_다른_카테고리에_상품을_각각_생성할_수_있다()` | S-PRD-2 | 성공 |
| `존재하지_않는_카테고리로_상품을_생성하면_실패하고_상품은_생성되지_않는다()` | F-PRD-1 | 실패 |
| `카테고리ID가_null이면_상품_생성이_실패하고_상품은_생성되지_않는다()` | F-PRD-2 | 실패 |
| `가격이_음수인_상품_생성_시_현재_행동을_확인한다()` | F-PRD-3 | 실패 |
| `가격이_0인_상품_생성_시_현재_행동을_확인한다()` | F-PRD-4 | 실패 |

**GiftAcceptanceTest** (시나리오 12개)

| 메서드명 | 시나리오 ID | 유형 |
|---------|-----------|------|
| `선물하기에_성공하면_재고가_차감된다()` | S-GIFT-1 | 성공 |
| `연속으로_선물하면_재고가_누적_차감된다()` | S-GIFT-2 | 성공 |
| `재고_전부를_선물하면_재고가_0이_된다()` | S-GIFT-3 | 성공 |
| `재고보다_많은_수량을_선물하면_실패하고_재고는_변하지_않는다()` | F-GIFT-1 | 실패 |
| `재고_소진_후_추가_선물은_실패하고_재고는_0을_유지한다()` | F-GIFT-2 | 실패 |
| `수량이_0인_선물하기_시_현재_행동을_확인한다()` | F-GIFT-3 | 실패 |
| `수량이_음수인_선물하기_시_현재_행동을_확인한다()` | F-GIFT-4 | 실패 |
| `존재하지_않는_옵션으로_선물하면_실패한다()` | F-GIFT-5 | 실패 |
| `존재하지_않는_보내는_회원으로_선물하면_실패하고_재고는_변하지_않는다()` | F-GIFT-6 | 실패 |
| `존재하지_않는_받는_회원에게_선물하기_시_현재_행동을_확인한다()` | F-GIFT-7 | 실패 |
| `Member_Id_헤더_없이_선물하면_실패하고_재고는_변하지_않는다()` | F-GIFT-8 | 실패 |
| `요청_바디_없이_선물하면_실패하고_재고는_변하지_않는다()` | F-GIFT-9 | 실패 |

### 테스트 총계

| 클래스 | 성공 | 실패 | 합계 |
|--------|------|------|------|
| CategoryAcceptanceTest | 2 | 1 | 3 |
| ProductAcceptanceTest | 2 | 4 | 6 |
| GiftAcceptanceTest | 3 | 9 | 12 |
| **합계** | **7** | **14** | **21** |
