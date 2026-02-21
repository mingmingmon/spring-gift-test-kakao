package gift;

import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * 카테고리 도메인 인수 테스트.
 *
 * [발견된 결함] CategoryRestController.create()에 @RequestBody가 없고,
 * CreateCategoryRequest에 setter가 없어 form parameter 바인딩이 작동하지 않는다.
 * 따라서 POST /api/categories로 전달한 name 값이 항상 null로 저장된다.
 *
 * 아래 테스트는 이 결함이 존재하는 현재 시스템의 실제 행동을 기록한다.
 */
class CategoryAcceptanceTest extends BaseAcceptanceTest {

    // S-CAT-1: @RequestBody 누락으로 인해 name이 바인딩되지 않는 현재 행동을 기록
    @Test
    @DisplayName("카테고리를 생성하면 조회할 수 있다 - 단, name은 바인딩되지 않음 (@RequestBody 누락)")
    void 카테고리를_생성하면_조회할_수_있다() {
        // Given & When: 카테고리 생성 (form param으로 name 전달)
        RestAssured.given()
                .param("name", "음료")
                .when()
                .post("/api/categories")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", nullValue()); // 결함: @RequestBody 없음 + setter 없음 → name 바인딩 불가

        // Then: 후속 조회로 레코드 존재 확인 (name은 null)
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].name", nullValue());
    }

    // S-CAT-2
    @Test
    @DisplayName("여러 카테고리를 생성하면 모두 조회된다 - 단, name은 모두 null")
    void 여러_카테고리를_생성하면_모두_조회된다() {
        // Given & When: 카테고리 3개 생성
        RestAssured.given().param("name", "음료").post("/api/categories");
        RestAssured.given().param("name", "간식").post("/api/categories");
        RestAssured.given().param("name", "선물세트").post("/api/categories");

        // Then: 3개 레코드 존재 (name은 모두 null)
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("", hasSize(3));
    }

    // F-CAT-1
    @Test
    @DisplayName("이름이 null인 카테고리 생성 요청은 실패하고 카테고리는 생성되지 않는다")
    void 이름이_null인_카테고리_생성_요청은_실패하고_카테고리는_생성되지_않는다() {
        // Given & When: name 파라미터 없이 카테고리 생성 요청
        RestAssured.given()
                .when()
                .post("/api/categories")
                .then()
                .statusCode(500);

        // Then: 카테고리가 생성되지 않았다 (상태 불변)
        RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }
}
