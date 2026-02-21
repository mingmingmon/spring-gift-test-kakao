# BUG_REPORT.md - 인수 테스트 결과 및 결함 수정 가이드

> 이 문서는 인수 테스트 실행 결과를 테스트 클래스별로 정리하고,
> 실패하는 테스트에 대해 원인과 수정 방법을 안내한다.

---

## 1. 테스트 클래스별 통과/실패 현황

### CategoryAcceptanceTest (3개 중 3개 실패)

| 시나리오 ID | 테스트 메서드 | 결과 | 실패 원인 |
|-----------|-------------|------|----------|
| S-CAT-1 | `카테고리를_생성하면_조회할_수_있다()` | **실패** | name="음료"를 기대하지만 @RequestBody 누락으로 null 바인딩 |
| S-CAT-2 | `여러_카테고리를_생성하면_모두_조회된다()` | **실패** | 각 name이 notNull을 기대하지만 null 바인딩 |
| F-CAT-1 | `이름이_null인_카테고리_생성_요청은_실패하고_카테고리는_생성되지_않는다()` | **실패** | 500을 기대하지만 null이 그대로 저장되어 200 반환 |

### ProductAcceptanceTest (6개 중 4개 통과, 2개 실패)

| 시나리오 ID | 테스트 메서드 | 결과 | 실패 원인 |
|-----------|-------------|------|----------|
| S-PRD-1 | `상품을_생성하면_조회할_수_있다()` | **실패** | 200을 기대하지만 @RequestBody 누락으로 categoryId=null → 500 |
| S-PRD-2 | `서로_다른_카테고리에_상품을_각각_생성할_수_있다()` | **실패** | 동일 원인으로 500 |
| F-PRD-1 | `존재하지_않는_카테고리로_상품을_생성하면_실패하고_상품은_생성되지_않는다()` | 통과 | |
| F-PRD-2 | `카테고리ID가_null이면_상품_생성이_실패하고_상품은_생성되지_않는다()` | 통과 | |
| F-PRD-3 | `가격이_음수인_상품_생성_요청은_실패하고_상품은_생성되지_않는다()` | 통과 | |
| F-PRD-4 | `가격이_0인_상품_생성_요청은_실패하고_상품은_생성되지_않는다()` | 통과 | |

> F-PRD-1~4는 @RequestBody 누락으로 categoryId가 null → findById(null) → 500이 발생하여 "실패"를 기대하는 테스트와 우연히 일치한다.
> BUG-1 수정 후에는 F-PRD-3, F-PRD-4가 새로 실패할 수 있다 (가격 검증 로직이 없으므로 음수/0 가격이 저장됨).

### GiftAcceptanceTest (12개 중 9개 통과, 3개 실패)

| 시나리오 ID | 테스트 메서드 | 결과 | 실패 원인 |
|-----------|-------------|------|----------|
| S-GIFT-1 | `선물하기에_성공하면_재고가_차감된다()` | 통과 | |
| S-GIFT-2 | `연속으로_선물하면_재고가_누적_차감된다()` | 통과 | |
| S-GIFT-3 | `재고_전부를_선물하면_재고가_0이_된다()` | 통과 | |
| F-GIFT-1 | `재고보다_많은_수량을_선물하면_실패하고_재고는_변하지_않는다()` | 통과 | |
| F-GIFT-2 | `재고_소진_후_추가_선물은_실패하고_재고는_0을_유지한다()` | 통과 | |
| F-GIFT-3 | `수량이_0인_선물하기_요청은_실패하고_재고는_변하지_않는다()` | **실패** | 500을 기대하지만 decrease(0)이 예외 없이 통과하여 200 반환 |
| F-GIFT-4 | `수량이_음수인_선물하기_요청은_실패하고_재고는_변하지_않는다()` | **실패** | 500을 기대하지만 decrease(-5)가 예외 없이 통과하여 200 반환 + 재고 증가 |
| F-GIFT-5 | `존재하지_않는_옵션으로_선물하면_실패한다()` | 통과 | |
| F-GIFT-6 | `존재하지_않는_보내는_회원으로_선물하면_실패하고_재고는_변하지_않는다()` | 통과 | |
| F-GIFT-7 | `존재하지_않는_받는_회원에게_선물하기_요청은_실패하고_재고는_변하지_않는다()` | **실패** | 500을 기대하지만 수신자 검증 없이 성공하여 200 반환 + 재고 차감 |
| F-GIFT-8 | `Member_Id_헤더_없이_선물하면_실패하고_재고는_변하지_않는다()` | 통과 | |
| F-GIFT-9 | `요청_바디_없이_선물하면_실패하고_재고는_변하지_않는다()` | 통과 | |

### 전체 요약

| 클래스 | 통과 | 실패 | 합계 |
|--------|------|------|------|
| CategoryAcceptanceTest | 0 | 3 | 3 |
| ProductAcceptanceTest | 4 | 2 | 6 |
| GiftAcceptanceTest | 9 | 3 | 12 |
| **합계** | **13** | **8** | **21** |

---

## 2. 실패 테스트별 원인 분석 및 수정 방법

### 2.1 S-CAT-1, S-CAT-2, S-PRD-1, S-PRD-2 — @RequestBody 누락

#### 원인

`CategoryRestController.java:23`과 `ProductRestController.java:23`의 POST 메서드에 `@RequestBody`가 없다.

```java
// CategoryRestController.java:23 (현재)
public Category create(final CreateCategoryRequest request) {

// ProductRestController.java:23 (현재)
public Product create(final CreateProductRequest request) {
```

`@RequestBody`가 없으면 Spring은 `@ModelAttribute` 방식으로 바인딩을 시도하는데, DTO에 setter가 없어 모든 필드가 기본값(null/0)으로 남는다.

- **Category**: name=null인 채로 DB에 저장됨 → 테스트가 기대하는 "음료"와 불일치
- **Product**: categoryId=null → `findById(null)` → `IllegalArgumentException` → 500

#### 수정 방법

```java
// CategoryRestController.java
@PostMapping
public Category create(@RequestBody final CreateCategoryRequest request) {
    return categoryService.create(request);
}

// ProductRestController.java
@PostMapping
public Product create(@RequestBody final CreateProductRequest request) {
    return productService.create(request);
}
```

---

### 2.2 F-CAT-1 — name=null 저장 허용

#### 원인

`CategoryService.java:19~21`에서 name에 대한 입력 검증 없이 그대로 저장한다.

```java
// CategoryService.java:19 (현재)
public Category create(final CreateCategoryRequest request) {
    return categoryRepository.save(new Category(request.getName()));
}
```

name이 null이어도 `new Category(null)` → DB 저장 → 200 OK가 반환된다.

> 이 버그는 BUG-1(@RequestBody 누락)을 수정한 뒤에도 별도로 수정이 필요하다.
> @RequestBody를 추가하면 JSON의 name 필드가 바인딩되지만, name을 아예 보내지 않으면 여전히 null이 저장된다.

#### 수정 방법

```java
// CategoryService.java
public Category create(final CreateCategoryRequest request) {
    if (request.getName() == null || request.getName().isBlank()) {
        throw new IllegalArgumentException("카테고리 이름은 필수입니다");
    }
    return categoryRepository.save(new Category(request.getName()));
}
```

---

### 2.3 F-GIFT-3, F-GIFT-4 — 음수/0 수량 허용

#### 원인

`Option.java:34~38`의 `decrease()` 메서드가 0 이하 수량을 거부하지 않는다.

```java
// Option.java:34 (현재)
public void decrease(final int quantity) {
    if (this.quantity < quantity) {
        throw new IllegalStateException();
    }
    this.quantity -= quantity;
}
```

- **quantity = 0**: 조건 `10 < 0` → false → 예외 없이 통과 → 재고 불변, 의미 없는 선물 성공
- **quantity = -5**: 조건 `10 < -5` → false → 예외 없이 통과 → `10 - (-5) = 15` → 재고 증가

#### 수정 방법

```java
// Option.java
public void decrease(final int quantity) {
    if (quantity <= 0) {
        throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다");
    }
    if (this.quantity < quantity) {
        throw new IllegalStateException("재고가 부족합니다");
    }
    this.quantity -= quantity;
}
```

---

### 2.4 F-GIFT-7 — 존재하지 않는 수신자에게 선물 성공

#### 원인

`FakeGiftDelivery.java:20~25`의 `deliver()` 메서드가 보내는 사람(`getFrom`)만 조회하고 받는 사람(`getTo`)은 조회하지 않는다.

```java
// FakeGiftDelivery.java:20 (현재)
@Override
public void deliver(final Gift gift) {
    final Member member = memberRepository.findById(gift.getFrom()).orElseThrow();
    final Option option = gift.getOption();
    final Product product = option.getProduct();
    System.out.println(member.getName() + product.getName() + option.getName() + option.getQuantity());
}
```

존재하지 않는 receiverId로 선물해도 검증 없이 성공하고, 재고만 차감된다.

#### 수정 방법

```java
// FakeGiftDelivery.java
@Override
public void deliver(final Gift gift) {
    final Member sender = memberRepository.findById(gift.getFrom()).orElseThrow();
    final Member receiver = memberRepository.findById(gift.getTo()).orElseThrow();
    final Option option = gift.getOption();
    final Product product = option.getProduct();
    System.out.println(sender.getName() + " → " + receiver.getName() + ": " + product.getName() + " " + option.getName());
}
```
