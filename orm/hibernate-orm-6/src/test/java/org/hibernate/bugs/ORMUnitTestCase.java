/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.bugs;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
				ORMUnitTestCase.Author.class, ORMUnitTestCase.Book.class,
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
				@Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "false"),
				@Setting(name = AvailableSettings.CRITERIA_PLAN_CACHE_ENABLED, value = "true"),
				// @Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),

				// Add your own settings that are a part of your quarkus configuration:
				// @Setting( name = AvailableSettings.SOME_CONFIGURATION_PROPERTY, value = "SOME_VALUE" ),
		}
)
@SessionFactory
class ORMUnitTestCase {

	// Add your tests, using standard JUnit 5.
	@Test
	void criteriaPlanCacheWithEntityParameters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author = populateData(session);

			assertThat(runQuery(session, author)).hasSize(5);
			assertThat(runQuery(session, author)).hasSize(5);
		} );
	}

	private static List<Book> runQuery(SessionImplementor session, Author author) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Book> q = cb.createQuery(Book.class);
		final JpaRoot<Book> root = q.from(Book.class);
		q.select(root);
		q.where(cb.equal(root.get("author"), author));
		return session.createQuery(q).getResultList();
	}

	public Author populateData(SessionImplementor entityManager) {
		final Author author = new Author();
		author.name = "David Gourley";
		entityManager.persist(author);

		for (int i = 0; i < 5; i++) {
			final Book book = new Book();
			book.name = "HTTP Definitive guide " + i;
			book.author = author;
			entityManager.persist(book);
			author.books.add(book);
		}

		return author;
	}

	@Entity(name = "Author")
	@Table(name = "Author")
	public static class Author {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long authorId;

		@Column
		public String name;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
		public List<Book> books = new ArrayList<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			final Author author = (Author) o;
			return authorId.equals(author.authorId);
		}

		@Override
		public int hashCode() {
			return authorId.hashCode();
		}
	}

	@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "Book")
	@Table(name = "Book")
	public static class Book {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long bookId;

		@Column
		public String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "author_id", nullable = false)
		public Author author;
	}
}
