package gift.cucumber;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;

public class DatabaseCleanUp {

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@Before(order = 0)
	public void cleanUp() {
		databaseCleaner.execute();
	}
}
