package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.Category
import com.dshatz.exposed_crud.models.CategoryTable
import com.dshatz.exposed_crud.models.CategoryTranslations
import com.dshatz.exposed_crud.models.CategoryTranslationsTable
import com.dshatz.exposed_crud.models.Director
import com.dshatz.exposed_crud.models.DirectorTable
import com.dshatz.exposed_crud.models.Language
import com.dshatz.exposed_crud.models.LanguageTable
import com.dshatz.exposed_crud.models.Movie
import com.dshatz.exposed_crud.models.MovieTable
import com.dshatz.exposed_crud.models.createWithRelated
import com.dshatz.exposed_crud.models.findById
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestDbBackReferences {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        // Use H2 in-memory database (to avoid timestamp precision issues)
        db = Database.connect("jdbc:h2:mem:test_db_${java.util.UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY", "org.h2.Driver")
        transaction(db) {
            // For in-memory H2 we can just create tables on each test run without dropping.
            // To satisfy FK constraints create parent tables first and child tables afterwards.
            listOf(
                DirectorTable,             // Parent
                LanguageTable,             // Parent
                CategoryTable,             // Parent
                MovieTable,                // Director, Category FK
                CategoryTranslationsTable, // Category, Language FK
            ).forEach {
                SchemaUtils.create(it)
            }
        }
        transaction(db) {
            println(SchemaUtils.listTables())
        }
    }

    @Test
    fun `back references`() {
        transaction(db) {
        val category = CategoryTable.repo.createReturning(Category())
        val catId = CategoryTranslationsTable.repo
            .withRelated(LanguageTable, CategoryTable)
            .createWithRelated(
                CategoryTranslations(
                    category.id,
                    "",
                    language = Language("lv"),
                    translation = "Latviski",
                )
            ).run {
                categoryId to languageCode
            }

        val translation = CategoryTranslationsTable.repo.withRelated(CategoryTable, LanguageTable).findById(catId.first, catId.second)
        translation?.category shouldBe category
        translation?.language?.code shouldBe "lv"

        CategoryTranslationsTable.repo
            .withRelated(LanguageTable)
            .createWithRelated(
                CategoryTranslations(
                    category.id,
                    "",
                    "In english",
                    language = Language("en")
                )
            )
        val withTranslations = CategoryTable.repo.withRelated(CategoryTranslationsTable).findById(category.id)
        withTranslations?.translations shouldNotBe null
        withTranslations?.translations?.size shouldBe 2
        withTranslations?.translations?.find { it.languageCode == "lv" }?.translation shouldBe "Latviski"
        withTranslations?.translations?.find { it.languageCode == "en" }?.translation shouldBe "In english"

        val director = DirectorTable.repo.createReturning(Director(name = "Alfred"))
        val movie1 = MovieTable.repo.createReturning(
            Movie(title = "The birds", directorId = director.id, categoryId = category.id, createdAt = kotlin.time.Clock.System.now()),
        )

        val movie2 = MovieTable.repo.createReturning(
            Movie(title = "The birds 2", directorId = director.id, categoryId = category.id, createdAt = kotlin.time.Clock.System.now()),
        )

        val directorWithMovies = DirectorTable.repo.withRelated(MovieTable).findById(director.id)
        directorWithMovies?.movies shouldNotBe null
        directorWithMovies?.movies?.toSet() shouldBe setOf(movie1, movie2)

        val categoryWithMovies = CategoryTable.repo.withRelated(MovieTable).findById(category.id)
        categoryWithMovies?.movies shouldNotBe null
        categoryWithMovies?.movies?.toSet() shouldBe setOf(movie1, movie2)
        }
    }
}

