package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;

/**
 * 상품 도메인 인수 테스트.
 *
 * [발견된 결함] ProductRestController.create()에 @RequestBody가 없고,
 * CreateProductRequest에 setter가 없어 form parameter 바인딩이 작동하지 않는다.
 * 따라서 categoryId가 항상 null이 되어 categoryRepository.findById(null)에서
 * IllegalArgumentException이 발생, 모든 상품 생성 요청이 500으로 실패한다.
 *
 * 아래 테스트는 이 결함이 존재하는 현재 시스템의 실제 행동을 기록한다.
 */
class ProductAcceptanceTest extends BaseAcceptanceTest {

    @Autowired
    CategoryRepository categoryRepository;

    // S-PRD-1: @RequestBody 누락으로 categoryId 바인딩 불가 → 500 실패
    @Test
    @DisplayName("상품 생성 요청은 @RequestBody 누락으로 항상 실패한다 (categoryId 바인딩 불가)")
    void 상품을_생성하면_조회할_수_있다() {
        // Given: 카테고리가 존재한다
        Category category = categoryRepository.save(new Category("음료"));

        // When: form param으로 상품 생성 요청 (categoryId 바인딩 불가 → null → findById(null) → 500)
        RestAssured.given()
                .param("name", "아메리카노")
                .param("price", 4500)
                .param("imageUrl", "http://image.png")
                .param("categoryId", category.getId())
                .when()
                .post("/api/products")
                .then()
                .statusCode(500); // 결함: @RequestBody 없음 + setter 없음 → categoryId 바인딩 불가

        // Then: 상품이 생성되지 않았다
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    // S-PRD-2: 같은 이유로 모든 상품 생성이 실패
    @Test
    @DisplayName("서로 다른 카테고리에 상품 생성도 @RequestBody 누락으로 실패한다")
    void 서로_다른_카테고리에_상품을_각각_생성할_수_있다() {
        // Given: 카테고리 2개 생성
        Category categoryA = categoryRepository.save(new Category("음료"));
        Category categoryB = categoryRepository.save(new Category("간식"));

        // When: 각 카테고리에 상품 생성 시도 (모두 실패)
        RestAssured.given()
                .param("name", "아메리카노")
                .param("price", 4500)
                .param("imageUrl", "http://image.png")
                .param("categoryId", categoryA.getId())
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        RestAssured.given()
                .param("name", "쿠키")
                .param("price", 3000)
                .param("imageUrl", "http://image.png")
                .param("categoryId", categoryB.getId())
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        // Then: 상품이 하나도 생성되지 않았다
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    // F-PRD-1: categoryId가 바인딩되지 않으므로 존재하지 않는 카테고리와 동일하게 500
    @Test
    @DisplayName("존재하지 않는 카테고리로 상품을 생성하면 실패하고 상품은 생성되지 않는다")
    void 존재하지_않는_카테고리로_상품을_생성하면_실패하고_상품은_생성되지_않는다() {
        // When: 존재하지 않는 카테고리로 상품 생성
        RestAssured.given()
                .param("name", "아메리카노")
                .param("price", 4500)
                .param("imageUrl", "http://image.png")
                .param("categoryId", 999999)
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        // Then: 상품이 생성되지 않았다 (상태 불변)
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    // F-PRD-2: categoryId 미전달 → 바인딩 불가 → null → 500
    @Test
    @DisplayName("카테고리ID가 null이면 상품 생성이 실패하고 상품은 생성되지 않는다")
    void 카테고리ID가_null이면_상품_생성이_실패하고_상품은_생성되지_않는다() {
        // When: categoryId 없이 상품 생성
        RestAssured.given()
                .param("name", "아메리카노")
                .param("price", 4500)
                .param("imageUrl", "http://image.png")
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        // Then: 상품이 생성되지 않았다 (상태 불변)
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    // F-PRD-3: 음수 가격이지만 categoryId 바인딩 불가로 가격 검증 이전에 실패
    @Test
    @DisplayName("가격이 음수인 상품 생성 시 현재 행동을 확인한다 - categoryId 바인딩 불가로 500")
    void 가격이_음수인_상품_생성_시_현재_행동을_확인한다() {
        // Given: 카테고리 생성
        Category category = categoryRepository.save(new Category("음료"));

        // When: 음수 가격으로 상품 생성 (categoryId 바인딩 불가 → 가격 검증 이전에 실패)
        RestAssured.given()
                .param("name", "아메리카노")
                .param("price", -1000)
                .param("imageUrl", "http://image.png")
                .param("categoryId", category.getId())
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        // Then: 상품이 생성되지 않았다
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    // F-PRD-4: 가격 0도 같은 이유로 실패
    @Test
    @DisplayName("가격이 0인 상품 생성 시 현재 행동을 확인한다 - categoryId 바인딩 불가로 500")
    void 가격이_0인_상품_생성_시_현재_행동을_확인한다() {
        // Given: 카테고리 생성
        Category category = categoryRepository.save(new Category("음료"));

        // When: 가격 0으로 상품 생성
        RestAssured.given()
                .param("name", "아메리카노")
                .param("price", 0)
                .param("imageUrl", "http://image.png")
                .param("categoryId", category.getId())
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);

        // Then: 상품이 생성되지 않았다
        RestAssured.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }
}
