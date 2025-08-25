package org.hibernate.bugs;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 * <p>
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
@DomainModel(
		annotatedClasses = {
				ORMUnitTestCase.Book.class
				// Add your entities here.
				// Foo.class,
				// Bar.class
		},
		// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
		xmlMappings = {
				// "org/hibernate/test/Foo.hbm.xml",
				// "org/hibernate/test/Bar.hbm.xml"
		}
)
@ServiceRegistry(
		// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
		settings = {
				// For your own convenience to see generated queries:
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
				@Setting(name = AvailableSettings.QUERY_PLAN_CACHE_ENABLED, value = "true"),
				@Setting(name = AvailableSettings.CRITERIA_PLAN_CACHE_ENABLED, value = "true"),
				@Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "false"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
		}
)
@SessionFactory
class ORMUnitTestCase {

	@Test
	void criteriaValueQueryPlanMiss(SessionFactoryScope scope) {
		// ValueBindJpaCriteriaParameter equality is based on value equality, rendering the query plan cache for criteria queries
		// virtually ineffective.

		scope.getSessionFactory().getStatistics().clear();
		scope.inTransaction(session -> {
			createBookSelectionQuery(session, "name1").getResultList();
			createBookSelectionQuery(session, "name2").getResultList();
			createBookSelectionQuery(session, "name3").getResultList();

			final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
			assertThat(stats.getQueryPlanCacheMissCount()).isEqualTo(1);
			assertThat(stats.getQueryPlanCacheHitCount()).isEqualTo(2);
		});
	}

	@Test
	void criteriaEmbedQueryPlanMiss(SessionFactoryScope scope) {
		// EmbeddedSqmPathSource acquires a unique alias, rendering the query plan cache ineffective 

		scope.getSessionFactory().getStatistics().clear();
		scope.inTransaction(session -> {
			createBookEmbedSelectionQuery(session).getResultList();
			createBookEmbedSelectionQuery(session).getResultList();
			createBookEmbedSelectionQuery(session).getResultList();

			final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
			assertThat(stats.getQueryPlanCacheMissCount()).isEqualTo(1);
			assertThat(stats.getQueryPlanCacheHitCount()).isEqualTo(2);
		});
	}

	private static SelectionQuery<Book> createBookSelectionQuery(SessionImplementor session, String name) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Book> q = cb.createQuery(Book.class);
		final Root<Book> root = q.from(Book.class);
		q.select(root);
		q.where(cb.equal(root.get("name"), name));
		return session.createSelectionQuery(q);
	}

	private static SelectionQuery<Book> createBookEmbedSelectionQuery(SessionImplementor session) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Book> q = cb.createQuery(Book.class);
		final Root<Book> root = q.from(Book.class);
		q.select(root);
		final Path<BookDetails> bookDetails = root.get("details"); // Note: the test passes with root.join("details")
		q.where(cb.equal(bookDetails.get("info"), "anyName"));
		return session.createSelectionQuery(q);
	}

	@Entity(name = "Book")
	@Table(name = "Book")
	public static class Book {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;

		@Column
		public String name;

		@Embedded
		public BookDetails details;
	}

	@Embeddable
	public static class BookDetails {
		@Column
		public String info;
	}


}
