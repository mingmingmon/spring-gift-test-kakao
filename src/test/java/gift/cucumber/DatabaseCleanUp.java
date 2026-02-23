package gift.cucumber;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseCleanUp {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Before(order = 0)  // 다른 @Before보다 먼저 실행
	public void cleanUp() {
		// 1. 외래키 제약 조건 비활성화 (H2)
		jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

		// 2. 모든 테이블 TRUNCATE
		jdbcTemplate.queryForList(
			"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'"
		).forEach(table -> {
			String tableName = (String) table.get("TABLE_NAME");
			jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
		});

		// 3. 외래키 제약 조건 다시 활성화
		jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
	}
}
