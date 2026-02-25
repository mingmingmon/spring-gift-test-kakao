package gift.cucumber;

import io.cucumber.java.Before;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class DatabaseCleanUp {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	EntityManager entityManager;

	@Before(order = 0)
	public void cleanUp() {
		List<String> tableNames = entityManager.getMetamodel().getEntities().stream()
			.map(EntityType::getJavaType)
			.filter(clazz -> clazz.isAnnotationPresent(Entity.class))
			.map(clazz -> {
				Table table = clazz.getAnnotation(Table.class);
				if (table != null && !table.name().isEmpty()) {
					return table.name();
				}
				return convertToSnakeCase(clazz.getSimpleName());
			})
			.toList();

		if (!tableNames.isEmpty()) {
			String truncateSql = "TRUNCATE TABLE " +
				String.join(", ", tableNames) +
				" CASCADE";
			jdbcTemplate.execute(truncateSql);
		}
	}

	private String convertToSnakeCase(String name) {
		return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}
}
