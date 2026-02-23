package gift.cucumber.steps;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefs {

	private final SharedContext context;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final long SENDER_ID = 1L;
	private static final long RECEIVER_ID = 2L;
	private static final long OPTION_ID = 1L;

	private Response lastGiftResponse;

	public GiftStepDefs(SharedContext context) {
		this.context = context;
	}

	@먼저("보내는 회원과 받는 회원이 존재한다")
	public void 회원이_존재한다() {
		jdbcTemplate.execute(
			"INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com') ON CONFLICT (id) DO NOTHING");
		jdbcTemplate.execute(
			"INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com') ON CONFLICT (id) DO NOTHING");
	}

	@먼저("상품과 옵션이 존재하고 재고는 {int}개이다")
	public void 상품과_옵션이_존재한다(int quantity) {
		jdbcTemplate.execute(
			"INSERT INTO category (id, name) VALUES (1, '테스트카테고리') ON CONFLICT (id) DO NOTHING");
		jdbcTemplate.execute(
			"INSERT INTO product (id, name, price, image_url, category_id) " +
				"VALUES (1, '테스트상품', 1000, 'http://img.png', 1) ON CONFLICT (id) DO NOTHING");
		jdbcTemplate.execute(
			"INSERT INTO \"option\" (id, name, quantity, product_id) " +
				"VALUES (1, '기본옵션', " + quantity + ", 1) ON CONFLICT (id) DO NOTHING");
	}

	@먼저("옵션의 재고를 {int}개로 설정한다")
	public void 옵션의_재고를_설정한다(int quantity) {
		jdbcTemplate.update("UPDATE \"option\" SET quantity = ? WHERE id = ?", quantity, OPTION_ID);
	}

	@만일("{int}개를 선물하면")
	public void N개를_선물하면(int quantity) {
		lastGiftResponse = 선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, quantity);
		context.setLastResponse(lastGiftResponse);
	}

	@그리고("{int}개를 추가로 선물하면")
	public void N개를_추가로_선물하면(int quantity) {
		lastGiftResponse = 선물하기_요청(SENDER_ID, OPTION_ID, RECEIVER_ID, quantity);
		context.setLastResponse(lastGiftResponse);
	}

	@만일("존재하지 않는 옵션으로 선물하면")
	public void 존재하지_않는_옵션으로_선물하면() {
		context.setLastResponse(
			선물하기_요청(SENDER_ID, 999999L, RECEIVER_ID, 1));
	}

	@만일("존재하지 않는 보내는 회원으로 {int}개를 선물하면")
	public void 존재하지_않는_보내는_회원으로_선물하면(int quantity) {
		context.setLastResponse(
			선물하기_요청(999999L, OPTION_ID, RECEIVER_ID, quantity));
	}

	@만일("존재하지 않는 받는 회원에게 {int}개를 선물하면")
	public void 존재하지_않는_받는_회원에게_선물하면(int quantity) {
		context.setLastResponse(
			선물하기_요청(SENDER_ID, OPTION_ID, 999999L, quantity));
	}

	@만일("Member-Id 헤더 없이 선물하면")
	public void MemberId_헤더_없이_선물하면() {
		context.setLastResponse(
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of(
					"optionId", OPTION_ID,
					"quantity", 3,
					"receiverId", RECEIVER_ID,
					"message", "선물입니다"
				))
				.post("/api/gifts")
		);
	}

	@만일("요청 바디 없이 선물하면")
	public void 요청_바디_없이_선물하면() {
		context.setLastResponse(
			RestAssured.given()
				.header("Member-Id", SENDER_ID)
				.contentType(ContentType.JSON)
				.post("/api/gifts")
		);
	}

	@그러면("마지막 응답 상태 코드는 {int}이다")
	public void 마지막_응답_상태_코드는(int statusCode) {
		lastGiftResponse.then().statusCode(statusCode);
	}

	@그리고("옵션의 재고는 {int}개이다")
	public void 옵션의_재고는_N개이다(int expectedQuantity) {
		int actual = jdbcTemplate.queryForObject(
			"SELECT quantity FROM \"option\" WHERE id = ?", Integer.class, OPTION_ID);
		assertThat(actual).isEqualTo(expectedQuantity);
	}

	private Response 선물하기_요청(long senderId, long optionId, long receiverId, int quantity) {
		return RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Member-Id", senderId)
			.body(Map.of(
				"optionId", optionId,
				"quantity", quantity,
				"receiverId", receiverId,
				"message", "선물입니다"
			))
			.post("/api/gifts");
	}
}
