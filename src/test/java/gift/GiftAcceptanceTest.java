package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GiftAcceptanceTest extends BaseAcceptanceTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    // S-GIFT-1
    @Test
    @DisplayName("선물하기에 성공하면 재고가 차감된다")
    void 선물하기에_성공하면_재고가_차감된다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(10);

        // When
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 3)
                .then()
                .statusCode(200);

        // Then: 재고가 10 → 7로 감소
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7);
    }

    // S-GIFT-2
    @Test
    @DisplayName("연속으로 선물하면 재고가 누적 차감된다")
    void 연속으로_선물하면_재고가_누적_차감된다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(10);

        // When: 3개 선물 후 4개 선물
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 3)
                .then().statusCode(200);
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 4)
                .then().statusCode(200);

        // Then: 재고가 10 → 7 → 3
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(3);
    }

    // S-GIFT-3
    @Test
    @DisplayName("재고 전부를 선물하면 재고가 0이 된다")
    void 재고_전부를_선물하면_재고가_0이_된다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(3);

        // When: 정확히 3개 선물
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 3)
                .then().statusCode(200);

        // Then: 재고가 정확히 0
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(0);
    }

    // F-GIFT-1
    @Test
    @DisplayName("재고보다 많은 수량을 선물하면 실패하고 재고는 변하지 않는다")
    void 재고보다_많은_수량을_선물하면_실패하고_재고는_변하지_않는다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(5);

        // When: 재고(5)보다 많은 10개 선물 시도
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 10)
                .then()
                .statusCode(500);

        // Then: 재고가 여전히 5 (상태 불변)
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(5);
    }

    // F-GIFT-2
    @Test
    @DisplayName("재고 소진 후 추가 선물은 실패하고 재고는 0을 유지한다")
    void 재고_소진_후_추가_선물은_실패하고_재고는_0을_유지한다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(3);

        // When: 3개 선물(성공) 후 1개 추가 시도
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 3)
                .then().statusCode(200);
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 1)
                .then().statusCode(500);

        // Then: 재고가 0 유지 (상태 불변)
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(0);
    }

    // F-GIFT-3
    @Test
    @DisplayName("수량이 0인 선물하기 시 현재 행동을 확인한다")
    void 수량이_0인_선물하기_시_현재_행동을_확인한다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(10);

        // When: quantity=0 선물 (현재 시스템은 검증 없이 성공)
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), 0)
                .then()
                .statusCode(200);

        // Then: 재고 불변 (decrease(0)은 아무 변화 없음)
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(10);
    }

    // F-GIFT-4
    @Test
    @DisplayName("수량이 음수인 선물하기 시 현재 행동을 확인한다")
    void 수량이_음수인_선물하기_시_현재_행동을_확인한다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(10);

        // When: quantity=-5 선물 (명확한 버그: 재고가 증가함)
        선물하기_요청(sender.getId(), option.getId(), receiver.getId(), -5)
                .then()
                .statusCode(200);

        // Then: 재고가 10 → 15로 증가 (버그 가시화)
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(15);
    }

    // F-GIFT-5
    @Test
    @DisplayName("존재하지 않는 옵션으로 선물하면 실패한다")
    void 존재하지_않는_옵션으로_선물하면_실패한다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));

        // When: 존재하지 않는 옵션으로 선물
        선물하기_요청(sender.getId(), 999999L, receiver.getId(), 1)
                .then()
                .statusCode(500);
    }

    // F-GIFT-6
    @Test
    @DisplayName("존재하지 않는 보내는 회원으로 선물하면 실패하고 재고는 변하지 않는다")
    void 존재하지_않는_보내는_회원으로_선물하면_실패하고_재고는_변하지_않는다() {
        // Given
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(10);

        // When: 존재하지 않는 회원(999999)이 선물
        선물하기_요청(999999L, option.getId(), receiver.getId(), 3)
                .then()
                .statusCode(500);

        // Then: 재고가 여전히 10 (트랜잭션 롤백 검증)
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(10);
    }

    // F-GIFT-7
    @Test
    @DisplayName("존재하지 않는 받는 회원에게 선물하기 시 현재 행동을 확인한다")
    void 존재하지_않는_받는_회원에게_선물하기_시_현재_행동을_확인한다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Option option = 옵션_생성(10);

        // When: 존재하지 않는 수신자(999999)에게 선물
        // FakeGiftDelivery는 getTo(받는사람)를 조회하지 않으므로 성공할 가능성이 높다
        선물하기_요청(sender.getId(), option.getId(), 999999L, 3)
                .then()
                .statusCode(200);

        // Then: 수신자 검증 없이 선물 성공 + 재고 차감됨 (결함 가능성)
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7);
    }

    // F-GIFT-8
    @Test
    @DisplayName("Member-Id 헤더 없이 선물하면 실패하고 재고는 변하지 않는다")
    void Member_Id_헤더_없이_선물하면_실패하고_재고는_변하지_않는다() {
        // Given
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Option option = 옵션_생성(10);

        // When: Member-Id 헤더 없이 요청
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 3,
                        "receiverId", receiver.getId(),
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(400);

        // Then: 재고 불변
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(10);
    }

    // F-GIFT-9
    @Test
    @DisplayName("요청 바디 없이 선물하면 실패하고 재고는 변하지 않는다")
    void 요청_바디_없이_선물하면_실패하고_재고는_변하지_않는다() {
        // Given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Option option = 옵션_생성(10);

        // When: body 없이 Member-Id 헤더만 포함하여 요청
        RestAssured.given()
                .header("Member-Id", sender.getId())
                .contentType(ContentType.JSON)
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(400);

        // Then: 재고 불변
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(10);
    }

    private Option 옵션_생성(int quantity) {
        Category category = categoryRepository.save(new Category("테스트카테고리"));
        Product product = productRepository.save(new Product("테스트상품", 10000, "http://image.png", category));
        return optionRepository.save(new Option("기본옵션", quantity, product));
    }

    private io.restassured.response.Response 선물하기_요청(Long senderId, Long optionId, Long receiverId, int quantity) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", senderId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts");
    }
}
