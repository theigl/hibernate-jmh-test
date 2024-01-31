package org.hibernate.bugs;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import javax.persistence.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
@State(Scope.Thread)
public class JPABenchmark {

	private EntityManagerFactory entityManagerFactory;

	@Setup
	public void setup() {
		entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");

		final EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		em.createQuery("delete Book").executeUpdate();
		em.createQuery("delete Author").executeUpdate();
		em.createQuery("delete AuthorDetails").executeUpdate();
		for (int i = 0; i < 1000; i++) {
			populateData(em);
		}
		em.getTransaction().commit();
		em.close();
	}

	@TearDown
	public void destroy() {
		entityManagerFactory.close();
	}

	@Benchmark
	public void perf5() {
		final EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		final List<Author> authors = em.createQuery("from Author", Author.class).getResultList();
		authors.forEach(author -> assertFalse(author.books.isEmpty()));
		em.getTransaction().commit();
		em.close();
	}

	@Benchmark
	public void perf5LargeTransaction() {
		final EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		em.setFlushMode(FlushModeType.COMMIT);
		for (int i = 0; i < 1_000; i++) {
			final List<Author> authors = em.createQuery("from Author", Author.class).getResultList();
			authors.forEach(author -> assertFalse(author.books.isEmpty()));
		}
		em.getTransaction().commit();
		em.close();
	}

	public static void main(String[] args) throws RunnerException, IOException {
		if (args.length == 0) {
			final Options opt = new OptionsBuilder()
					.include(".*" + JPABenchmark.class.getSimpleName() + ".*")
					.warmupIterations(3)
					.warmupTime(TimeValue.seconds(3))
					.measurementIterations(3)
					.measurementTime(TimeValue.seconds(5))
					.threads(1)
					.addProfiler("gc")
					.forks(2)
					.build();
			new Runner(opt).run();
		} else {
			Main.main(args);
		}
	}

	public void populateData(EntityManager entityManager) {
		final Book book = new Book();
		book.name = "HTTP Definitive guide";

		final Author author = new Author();
		author.name = "David Gourley";

		final AuthorDetails details = new AuthorDetails();
		details.name = "Author Details";
		details.author = author;
		author.details = details;

		author.books.add(book);
		book.author = author;

		entityManager.persist(author);
		entityManager.persist(book);
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

		@OneToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
		public AuthorDetails details;

	}

	@Entity(name = "AuthorDetails")
	@Table(name = "AuthorDetails")
	public static class AuthorDetails {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long detailsId;

		@Column
		public String name;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "details", optional = false)
		public Author author;
	}

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
