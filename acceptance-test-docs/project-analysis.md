# Project Analysis - Spring Gift Test Kakao

## 1. 프로젝트 구조

```
spring-gift-test-kakao/
├── build.gradle                          # Gradle 빌드 설정
├── settings.gradle                       # 프로젝트명: spring-gift-test
├── CLAUDE.md                             # 행동 기반 테스트 전략 문서
├── src/
│   ├── main/
│   │   ├── java/gift/
│   │   │   ├── Application.java          # Spring Boot 진입점
│   │   │   ├── model/                    # 도메인 모델 + 리포지토리
│   │   │   ├── application/              # 서비스 + Request DTO
│   │   │   ├── infrastructure/           # 외부 연동 구현체
│   │   │   └── ui/                       # REST 컨트롤러
│   │   └── resources/
│   │       └── application.properties    # 앱 설정
│   └── test/
│       └── java/gift/.gitkeep            # 테스트 미작성 (빈 디렉토리)
```

### 기술 스택
- Java 21
- Spring Boot 3.5.8
- Spring Data JPA
- H2 Database (in-memory)
- Thymeleaf (의존성 존재하나 사용되지 않음)
- Gradle 8.4

---

## 2. 계층 구조

```
┌─────────────────────────────────────────┐
│  UI Layer (ui/)                         │
│  - CategoryRestController               │
│  - ProductRestController                │
│  - GiftRestController                   │
├─────────────────────────────────────────┤
│  Application Layer (application/)       │
│  - CategoryService                      │
│  - ProductService                       │
│  - OptionService                        │
│  - WishService                          │
│  - GiftService                          │
│  - Request DTOs (Create*Request, etc.)  │
├─────────────────────────────────────────┤
│  Domain Model Layer (model/)            │
│  - Category, Product, Option            │
│  - Member, Wish, Gift                   │
│  - GiftDelivery (interface)             │
│  - *Repository (JPA interfaces)         │
├─────────────────────────────────────────┤
│  Infrastructure Layer (infrastructure/) │
│  - FakeGiftDelivery                     │
│  - KakaoMessageProperties               │
│  - KakaoSocialProperties                │
└─────────────────────────────────────────┘
```

의존 방향: UI → Application → Model ← Infrastructure

---

## 3. 도메인 모델 및 엔티티 정보

### 3.1 Category (JPA Entity)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK, IDENTITY 전략 |
| name | String | 카테고리명 |

### 3.2 Product (JPA Entity)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK, IDENTITY 전략 |
| name | String | 상품명 |
| price | int | 가격 |
| imageUrl | String | 이미지 URL |
| category | Category | ManyToOne, 소속 카테고리 |

### 3.3 Option (JPA Entity)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK, IDENTITY 전략 |
| name | String | 옵션명 |
| quantity | int | 재고 수량 |
| product | Product | ManyToOne, 소속 상품 |

핵심 비즈니스 로직:
- `decrease(int quantity)`: 재고 차감. 재고 부족 시 `IllegalStateException` 발생

### 3.4 Member (JPA Entity)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK, IDENTITY 전략 |
| name | String | 회원명 |
| email | String | 이메일 |

### 3.5 Wish (JPA Entity)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK, IDENTITY 전략 |
| member | Member | ManyToOne, 위시리스트 소유자 |
| product | Product | ManyToOne, 위시 상품 |

### 3.6 Gift (일반 객체, 비영속)
| 필드 | 타입 | 설명 |
|------|------|------|
| from | Long | 선물 보내는 회원 ID |
| to | Long | 선물 받는 회원 ID |
| option | Option | 선물 옵션 |
| quantity | int | 수량 |
| message | String | 메시지 |

### 3.7 GiftDelivery (인터페이스)
- `deliver(Gift gift)`: 선물 배송 추상화
- 구현체: `FakeGiftDelivery` (콘솔 출력만 수행)

### 엔티티 관계도

```
Category ──< Product ──< Option
                │
Member ────< Wish ─────┘
   │
   └──── Gift (비영속) ────> Option
```

---

## 4. 사용 가능한 API

### 4.1 Category API

| Method | URI | 설명 | Request Body | Response |
|--------|-----|------|-------------|----------|
| POST | `/api/categories` | 카테고리 생성 | `CreateCategoryRequest` | `Category` |
| GET | `/api/categories` | 카테고리 전체 조회 | - | `List<Category>` |

### 4.2 Product API

| Method | URI | 설명 | Request Body | Response |
|--------|-----|------|-------------|----------|
| POST | `/api/products` | 상품 생성 | `CreateProductRequest` | `Product` |
| GET | `/api/products` | 상품 전체 조회 | - | `List<Product>` |

### 4.3 Gift API

| Method | URI | 설명 | Request Body | Headers | Response |
|--------|-----|------|-------------|---------|----------|
| POST | `/api/gifts` | 선물하기 | `GiveGiftRequest` | `Member-Id: {memberId}` | void (200) |

### 4.4 API가 존재하지 않는 기능 (컨트롤러 미구현)

| 기능 | Service 존재 | Controller 존재 |
|------|:-----------:|:---------------:|
| Option 생성/조회 | O (`OptionService`) | **X** |
| Wish 생성 | O (`WishService`) | **X** |
| Member 생성/조회 | **X** | **X** |

---

## 5. 결함 및 의심 사항

### 5.1 [심각] Controller에서 `@RequestBody` 누락

**CategoryRestController.create()** 와 **ProductRestController.create()** 에서 `@RequestBody` 어노테이션이 없음.

```java
// CategoryRestController
@PostMapping
public Category create(final CreateCategoryRequest request) {  }

// ProductRestController
@PostMapping
public Product create(final CreateProductRequest request) {  }
```

- `@RequestBody`가 없으면 Spring은 JSON body가 아닌 form parameter로 바인딩을 시도함
- JSON으로 요청을 보내면 필드가 모두 `null`/`0`으로 바인딩될 가능성이 높음
- **GiftRestController만 올바르게 `@RequestBody`를 사용하고 있음**

### 5.2 [심각] Option API 컨트롤러 미구현

- `OptionService`는 존재하지만 이를 노출하는 컨트롤러가 없음
- Option을 생성할 수 있는 API가 없으므로, Gift 기능이 사실상 동작 불가능
- 선물하기(`POST /api/gifts`)는 `optionId`를 요구하지만, Option을 만들 방법이 API로 제공되지 않음

### 5.3 [심각] Member API 미구현

- `Member` 엔티티와 `MemberRepository`는 존재하지만 Service와 Controller가 모두 없음
- Gift API는 `Member-Id` 헤더를 요구하고, WishService는 `memberId`를 요구함
- Member를 생성할 수 있는 API가 없어 Gift/Wish 기능이 사실상 동작 불가능

### 5.4 [심각] Wish API 컨트롤러 미구현

- `WishService`는 존재하지만 이를 노출하는 컨트롤러가 없음
- Wish 생성과 조회가 외부에서 불가능

### 5.5 [중간] 에러 처리 전략 부재

- 모든 Service에서 `orElseThrow()`를 사용하지만, 커스텀 예외나 글로벌 예외 핸들러가 없음
- 존재하지 않는 리소스 접근 시 `NoSuchElementException`이 발생하며, 이는 500 Internal Server Error로 응답됨
- 클라이언트는 400(잘못된 요청)과 404(리소스 없음)를 구분할 수 없음

### 5.6 [중간] GiftService의 트랜잭션 범위와 외부 호출

```java
@Transactional
public void give(final GiveGiftRequest request, final Long memberId) {
    option.decrease(request.getQuantity());   // DB 변경
    giftDelivery.deliver(gift);               // 외부 호출 (트랜잭션 내부)
}
```

- `giftDelivery.deliver()`가 트랜잭션 내부에서 실행됨
- 외부 호출이 실패하면 재고 차감도 롤백되는 구조
- 하지만 외부 호출이 성공했는데 이후 트랜잭션 커밋이 실패하면, 선물은 전달되었으나 재고는 차감되지 않는 불일치 발생 가능

### 5.7 [낮음] Gift 객체의 영속 미지원

- `Gift`는 JPA 엔티티가 아닌 일반 객체
- 선물 이력이 DB에 저장되지 않아, 선물 내역 조회가 불가능
- 시스템 장애 시 선물 이력 추적 불가

### 5.8 [낮음] Option.decrease()의 에러 메시지 부재

```java
public void decrease(final int quantity) {
    if (this.quantity < quantity) {
        throw new IllegalStateException();  // 메시지 없음
    }
    this.quantity -= quantity;
}
```

- 예외에 메시지가 없어 디버깅과 에러 응답이 불친절함

### 5.9 [낮음] Thymeleaf 의존성 미사용

- `build.gradle`에 `spring-boot-starter-thymeleaf`가 포함되어 있으나, 템플릿 파일이나 View Controller가 없음

### 5.10 [낮음] Kakao Properties 미사용

- `KakaoMessageProperties`, `KakaoSocialProperties`가 정의되어 있고 `application.properties`에 값이 설정되어 있으나, 실제로 사용하는 코드가 없음
- 현재 `FakeGiftDelivery`가 Kakao API 대신 콘솔 출력을 수행

---

## 6. 핵심 행동 식별 (CLAUDE.md Step 1)

CLAUDE.md 기준에 따라 다음 핵심 행동을 선정한다.

### 선정 기준
- 상태 변화가 발생하는 기능
- 비즈니스 리스크가 큰 기능
- 실패 시나리오가 명확한 기능
- 리팩터링 시 깨질 가능성이 높은 경계 기능

### 핵심 행동 목록

| # | 행동 | 상태 변화 | 선정 이유 |
|---|------|----------|----------|
| 1 | **카테고리 생성** | Category 레코드 생성 | 상품 생성의 전제 조건. 카테고리 없이는 상품을 만들 수 없으므로 데이터 무결성에 영향 |
| 2 | **상품 생성** | Product 레코드 생성 (Category 참조) | 존재하지 않는 카테고리로 상품 생성 시 실패해야 하는 경계 조건 존재 |
| 3 | **상품 조회** | 없음 (읽기) | 생성된 상품이 실제로 조회되는지 확인하여 생성 행동의 결과를 검증하는 수단 |
| 4 | **선물하기 (재고 차감)** | Option.quantity 감소 | 핵심 비즈니스 로직. 재고 차감 + 외부 배송 연동이 트랜잭션으로 묶여있어 리스크가 가장 높음 |
| 5 | **선물하기 실패 (재고 부족)** | 상태 불변 | 재고 부족 시 요청 실패 + 재고 미변경을 검증해야 함. 트랜잭션 롤백의 간접 증명 |

### 비고: API 미구현으로 인한 테스트 제약

현재 Option, Member, Wish에 대한 API가 없어 다음과 같은 제약이 있다:

- **Gift 테스트**: Option과 Member를 API로 준비할 수 없어, 테스트 전용 데이터 셋업이 필요
- **Wish 테스트**: Controller가 없어 API 경계 테스트 자체가 불가능

> 인수 테스트 작성 시 이 제약을 해결하기 위한 데이터 준비 전략이 필요하다.
> (CLAUDE.md Step 3에서 다룸)
