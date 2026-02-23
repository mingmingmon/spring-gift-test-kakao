package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Sql({"classpath:sql/truncate.sql", "classpath:sql/gift-data.sql"})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class GiftAcceptanceTest extends BaseAcceptanceTest {

    private static final long SENDER_ID = 1L;
    private static final long RECEIVER_ID = 2L;
    private static final long OPTION_ID = 1L;

    // S-GIFT-1
    @Test
    @DisplayName("선물하기에 성공하면 재고가 차감된다")
    void 선물하기에_성공하면_재고가_차감된다() {
        // When
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 3)
                .then()
                .statusCode(200);

        // Then: 재고가 10 → 7로 감소
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(7);
    }

    // S-GIFT-2
    @Test
    @DisplayName("연속으로 선물하면 재고가 누적 차감된다")
    void 연속으로_선물하면_재고가_누적_차감된다() {
        // When: 3개 선물 후 4개 선물
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 3)
                .then().statusCode(200);
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 4)
                .then().statusCode(200);

        // Then: 재고가 10 → 7 → 3
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(3);
    }

    // S-GIFT-3
    @Test
    @DisplayName("재고 전부를 선물하면 재고가 0이 된다")
    @Sql(statements = "UPDATE option SET quantity = 3 WHERE id = 1")
    void 재고_전부를_선물하면_재고가_0이_된다() {
        // When: 정확히 3개 선물
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 3)
                .then().statusCode(200);

        // Then: 재고가 정확히 0
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(0);
    }

    // F-GIFT-1
    @Test
    @DisplayName("재고보다 많은 수량을 선물하면 실패하고 재고는 변하지 않는다")
    @Sql(statements = "UPDATE option SET quantity = 5 WHERE id = 1")
    void 재고보다_많은_수량을_선물하면_실패하고_재고는_변하지_않는다() {
        // When: 재고(5)보다 많은 10개 선물 시도
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 10)
                .then()
                .statusCode(500);

        // Then: 재고가 여전히 5 (상태 불변)
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(5);
    }

    // F-GIFT-2
    @Test
    @DisplayName("재고 소진 후 추가 선물은 실패하고 재고는 0을 유지한다")
    @Sql(statements = "UPDATE option SET quantity = 3 WHERE id = 1")
    void 재고_소진_후_추가_선물은_실패하고_재고는_0을_유지한다() {
        // When: 3개 선물(성공) 후 1개 추가 시도
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 3)
                .then().statusCode(200);
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 1)
                .then().statusCode(500);

        // Then: 재고가 0 유지 (상태 불변)
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(0);
    }

    // F-GIFT-3
    @Test
    @DisplayName("수량이 0인 선물하기 요청은 실패하고 재고는 변하지 않는다")
    void 수량이_0인_선물하기_요청은_실패하고_재고는_변하지_않는다() {
        // When: quantity=0 선물 요청
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, 0)
                .then()
                .statusCode(500);

        // Then: 재고 불변 (상태 불변)
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(10);
    }

    // F-GIFT-4
    @Test
    @DisplayName("수량이 음수인 선물하기 요청은 실패하고 재고는 변하지 않는다")
    void 수량이_음수인_선물하기_요청은_실패하고_재고는_변하지_않는다() {
        // When: quantity=-5 선물 요청
        선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, -5)
                .then()
                .statusCode(500);

        // Then: 재고 불변 (상태 불변)
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(10);
    }

    // F-GIFT-5
    @Test
    @DisplayName("존재하지 않는 옵션으로 선물하면 실패한다")
    void 존재하지_않는_옵션으로_선물하면_실패한다() {
        // When: 존재하지 않는 옵션으로 선물
        선물하기_요청(SENDER_ID, 999999L, RECEIVER_ID, 1)
                .then()
                .statusCode(500);
    }

    // F-GIFT-6
    @Test
    @DisplayName("존재하지 않는 보내는 회원으로 선물하면 실패하고 재고는 변하지 않는다")
    void 존재하지_않는_보내는_회원으로_선물하면_실패하고_재고는_변하지_않는다() {
        // When: 존재하지 않는 회원(999999)이 선물
        선물하기_요청(999999L, OPTION_ID, RECEIVER_ID, 3)
                .then()
                .statusCode(500);

        // Then: 재고가 여전히 10 (트랜잭션 롤백 검증)
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(10);
    }

    // F-GIFT-7
    @Test
    @DisplayName("존재하지 않는 받는 회원에게 선물하기 요청은 실패하고 재고는 변하지 않는다")
    void 존재하지_않는_받는_회원에게_선물하기_요청은_실패하고_재고는_변하지_않는다() {
        // When: 존재하지 않는 수신자(999999)에게 선물 요청
        선물하기_요청(SENDER_ID, OPTION_ID, 999999L, 3)
                .then()
                .statusCode(500);

        // Then: 재고 불변 (상태 불변)
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(10);
    }

    // F-GIFT-8
    @Test
    @DisplayName("Member-Id 헤더 없이 선물하면 실패하고 재고는 변하지 않는다")
    void Member_Id_헤더_없이_선물하면_실패하고_재고는_변하지_않는다() {
        // When: Member-Id 헤더 없이 요청
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "optionId", OPTION_ID,
                        "quantity", 3,
                        "receiverId", RECEIVER_ID,
                        "message", "선물입니다"
                ))
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(400);

        // Then: 재고 불변
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(10);
    }

    // F-GIFT-9
    @Test
    @DisplayName("요청 바디 없이 선물하면 실패하고 재고는 변하지 않는다")
    void 요청_바디_없이_선물하면_실패하고_재고는_변하지_않는다() {
        // When: body 없이 Member-Id 헤더만 포함하여 요청
        RestAssured.given()
                .header("Member-Id", SENDER_ID)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/gifts")
                .then()
                .statusCode(400);

        // Then: 재고 불변
        assertThat(옵션_수량_조회(OPTION_ID)).isEqualTo(10);
    }

    private int 옵션_수량_조회(long optionId) {
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM option WHERE id = ?", Integer.class, optionId);
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
