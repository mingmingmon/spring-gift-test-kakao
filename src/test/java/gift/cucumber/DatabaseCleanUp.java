package gift.cucumber;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class DatabaseCleanUp {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Before(order = 0)
	public void cleanUp() {
		List<String> tableNames = jdbcTemplate.queryForList(
			"SELECT table_name FROM information_schema.tables " +
				"WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
			String.class
		);

		if (!tableNames.isEmpty()) {
			String truncateSql = "TRUNCATE TABLE " +
				String.join(", ", tableNames) +
				" CASCADE";
			jdbcTemplate.execute(truncateSql);
		}
	}
}
