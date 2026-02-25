package gift.cucumber;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleaner {

	private final List<JpaRepository<?, Long>> jpaRepositories;
	private final EntityManager entityManager;

	public DatabaseCleaner(List<JpaRepository<?, Long>> jpaRepositories, EntityManager entityManager) {
		this.jpaRepositories = jpaRepositories;
		this.entityManager = entityManager;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void execute() {
		Session session = entityManager.unwrap(Session.class);
		session.doWork(connection -> {
			connection.prepareStatement("SET session_replication_role = 'replica'").execute();
			jpaRepositories.forEach(JpaRepository::deleteAllInBatch);
			connection.prepareStatement("SET session_replication_role = 'origin'").execute();
		});
	}
}
