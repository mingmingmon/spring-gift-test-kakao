package gift.cucumber.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.먼저;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class CategoryStepDefs {

	private final SharedContext context;

	public CategoryStepDefs(SharedContext context) {
		this.context = context;
	}

	@먼저("{string} 카테고리가 존재한다")
	public void 카테고리가_존재한다(String name) {
		long categoryId = RestAssured.given()
			.contentType(ContentType.JSON)
			.body(Map.of("name", name))
			.post("/api/categories")
			.then()
			.statusCode(200)
			.extract()
			.jsonPath()
			.getLong("id");
		context.setSavedCategoryId(categoryId);
	}

	@만일("{string} 이름으로 카테고리를 생성하면")
	public void 이름으로_카테고리를_생성하면(String name) {
		context.setLastResponse(
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of("name", name))
				.post("/api/categories")
		);
	}

	@만일("이름 없이 카테고리를 생성하면")
	public void 이름없이_카테고리를_생성하면() {
		context.setLastResponse(
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of())
				.post("/api/categories")
		);
	}

	@만일("다음 카테고리들을 생성한다:")
	public void 다음_카테고리들을_생성한다(DataTable table) {
		table.asMaps().forEach(row ->
			RestAssured.given()
				.contentType(ContentType.JSON)
				.body(Map.of("name", row.get("name")))
				.post("/api/categories")
		);
	}

	@그러면("응답 상태 코드는 {int}이다")
	public void 응답_상태_코드는(int statusCode) {
		context.getLastResponse().then().statusCode(statusCode);
	}

	@그리고("카테고리 목록을 조회하면 {int}개가 존재한다")
	public void 카테고리_목록을_조회하면_N개가_존재한다(int count) {
		RestAssured.given()
			.get("/api/categories")
			.then()
			.statusCode(200)
			.body("", hasSize(count));
	}

	@그리고("첫 번째 카테고리의 이름은 {string}이다")
	public void 첫번째_카테고리의_이름은(String name) {
		RestAssured.given()
			.get("/api/categories")
			.then()
			.body("[0].name", equalTo(name));
	}
}
