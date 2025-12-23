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
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestDBBackReferences {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        // H2 데이터베이스 사용 (타임스탬프 정밀도 문제 해결을 위해)
        db = Database.connect("jdbc:h2:mem:test_db_${java.util.UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY", "org.h2.Driver")
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // H2 메모리 DB는 매 테스트마다 새로 만들어지므로 drop 없이 create만 하면 된다.
            // FK 제약 때문에 부모 테이블을 먼저 생성하고, 자식 테이블을 나중에 생성한다.
            listOf(
                DirectorTable,            // 부모 테이블
                LanguageTable,            // 부모 테이블
                CategoryTable,            // 부모 테이블
                MovieTable,               // Director, Category FK 참조
                CategoryTranslationsTable, // Category, Language FK 참조
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
            Movie(title = "The birds", directorId = director.id, categoryId = category.id, createdAt = Clock.System.now()),
        )

        val movie2 = MovieTable.repo.createReturning(
            Movie(title = "The birds 2", directorId = director.id, categoryId = category.id, createdAt = Clock.System.now()),
        )

        val directorWithMovies = DirectorTable.repo.withRelated(MovieTable).findById(director.id)
        directorWithMovies?.movies shouldNotBe null
        directorWithMovies!!.movies!!.toSet() shouldBe setOf(movie1, movie2)

        val categoryWithMovies = CategoryTable.repo.withRelated(MovieTable).findById(category.id)
        categoryWithMovies?.movies shouldNotBe null
        categoryWithMovies!!.movies?.toSet() shouldBe setOf(movie1, movie2)
        }
    }
}

