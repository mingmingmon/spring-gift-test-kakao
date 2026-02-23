package gift.cucumber.steps;

import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ProductStepDefs {

	private final SharedContext context;

	public ProductStepDefs(SharedContext context) {
		this.context = context;
	}

	@만일("해당 카테고리에 {string} 상품을 가격 {int}원으로 생성하면")
	public void 해당_카테고리에_상품을_생성하면(String name, int price) {
		context.setLastResponse(
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of(
					"name", name,
					"price", price,
					"imageUrl", "http://image.png",
					"categoryId", context.getSavedCategoryId()
				))
				.post("/api/products")
		);
	}

	@그리고("{string} 카테고리에 {string} 상품을 가격 {int}원으로 생성한다")
	public void 카테고리에_상품을_생성한다(String categoryName, String productName, int price) {
		// 먼저 카테고리 ID를 이름으로 조회
		long categoryId = RestAssured.given()
			.get("/api/categories")
			.then()
			.extract()
			.jsonPath()
			.getLong("find { it.name == '" + categoryName + "' }.id");

		context.setLastResponse(
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of(
					"name", productName,
					"price", price,
					"imageUrl", "http://image.png",
					"categoryId", categoryId
				))
				.post("/api/products")
		);
	}

	@만일("존재하지 않는 카테고리로 상품을 생성하면")
	public void 존재하지_않는_카테고리로_상품을_생성하면() {
		context.setLastResponse(
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of(
					"name", "아메리카노",
					"price", 4500,
					"imageUrl", "http://image.png",
					"categoryId", 999999
				))
				.post("/api/products")
		);
	}

	@만일("카테고리ID 없이 상품을 생성하면")
	public void 카테고리ID_없이_상품을_생성하면() {
		context.setLastResponse(
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of(
					"name", "아메리카노",
					"price", 4500,
					"imageUrl", "http://image.png"
				))
				.post("/api/products")
		);
	}

	@그리고("상품 목록을 조회하면 {int}개가 존재한다")
	public void 상품_목록을_조회하면_N개가_존재한다(int count) {
		RestAssured.given()
			.get("/api/products")
			.then()
			.statusCode(200)
			.body("", hasSize(count));
	}

	@그리고("첫 번째 상품의 이름은 {string}이고 가격은 {int}원이다")
	public void 첫번째_상품의_이름과_가격(String name, int price) {
		RestAssured.given()
			.get("/api/products")
			.then()
			.body("[0].name", equalTo(name))
			.body("[0].price", equalTo(price));
	}
}
