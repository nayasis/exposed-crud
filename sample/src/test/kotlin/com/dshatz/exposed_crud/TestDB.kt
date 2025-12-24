package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.Category
import com.dshatz.exposed_crud.models.CategoryTable
import com.dshatz.exposed_crud.models.CategoryTranslations
import com.dshatz.exposed_crud.models.CategoryTranslationsTable
import com.dshatz.exposed_crud.models.Color
import com.dshatz.exposed_crud.models.ConvertedEntity
import com.dshatz.exposed_crud.models.ConvertedEntityTable
import com.dshatz.exposed_crud.models.Director
import com.dshatz.exposed_crud.models.DirectorTable
import com.dshatz.exposed_crud.models.IgnoredFieldEntityTable
import com.dshatz.exposed_crud.models.Language
import com.dshatz.exposed_crud.models.LanguageTable
import com.dshatz.exposed_crud.models.Movie
import com.dshatz.exposed_crud.models.MovieTable
import com.dshatz.exposed_crud.models.createWithRelated
import com.dshatz.exposed_crud.models.deleteById
import com.dshatz.exposed_crud.models.findById
import com.dshatz.exposed_crud.models.repo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestDB {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect("jdbc:sqlite:memory:test_db_${java.util.UUID.randomUUID()}?foreign_keys=on", "org.sqlite.JDBC")
        transaction(db) {
            listOf(
                DirectorTable,
                MovieTable,
                LanguageTable,
                CategoryTable,
                CategoryTranslationsTable,
                ConvertedEntityTable,
            ).forEach {
                SchemaUtils.drop(it)
                SchemaUtils.create(it)
            }
        }
        transaction(db) {
            println(SchemaUtils.listTables())
        }
    }

    @Test
    fun `test insert`() {
        transaction(db) {
            val director = Director(0, "Alfred")
            val inserted = DirectorTable.repo.createReturning(director.copy(oldDirector = director))
            val found = DirectorTable.repo.select().where(DirectorTable.name eq "Alfred").first()
            found.name shouldBe "Alfred"
            found shouldBe inserted
        }
    }

    @Test
    fun `typed select all`() {
        transaction(db) {
            DirectorTable.insert {
                it[name] = "Alfred"
            }
            val all = DirectorTable.repo.selectAll()
            all.size shouldBe 1
            all.first().name shouldBe "Alfred"
        }
    }

    @Test
    fun `typed insert`() {
        transaction(db) {
            DirectorTable.repo.create(Director(name = "Bob"))
            DirectorTable.repo.selectAll().first().name shouldBe "Bob"
        }
    }

    @Test
    fun `update custom where`() {
        transaction(db) {
            DirectorTable.repo.create(Director(name = "Bob"))
            val id = DirectorTable.repo.selectAll().find { it.name == "Bob" }!!.id

            DirectorTable.repo.update({ DirectorTable.id eq id }, Director(name = "Marley"))

            DirectorTable.repo.selectAll().first().name shouldBe "Marley"
        }
    }

    @Test
    fun `update by simple primary key`() {
        transaction(db) {
            val id = DirectorTable.repo.createReturning(Director(name = "Bob")).id
            DirectorTable.repo.update(Director(id, "Marley"))
            DirectorTable.repo.selectAll().first().name shouldBe "Marley"
        }
    }

    @Test
    fun `select where`() {
        transaction(db) {
            val id = DirectorTable.repo.createReturning(Director(name = "Bob")).id
            val director = DirectorTable.repo.select().where {
                DirectorTable.name eq "Bob"
            }.first()

            director.id shouldBe id
            director.name shouldBe "Bob"
        }
    }

    @Test
    fun `find by id`() {
        transaction(db) {
            val directorId = DirectorTable.repo.createReturning(Director(name = "Alfred")).id

            val found = DirectorTable.repo.findById(directorId)
            found shouldNotBe null
            found!!.name shouldBe "Alfred"
        }
    }

    @Test
    fun `composite ids`() {
        transaction(db) {
            val lang = "lv"
            LanguageTable.repo.create(Language(lang))
            val catId = CategoryTable.repo.createReturning(Category()).id
            CategoryTranslationsTable.repo.create(
                CategoryTranslations(
                    catId, lang, "Latviski"
                )
            )
            val found = CategoryTranslationsTable.repo.findById(catId, lang)
            found?.translation shouldBe "Latviski"

            val withTranslations = CategoryTable.repo.withRelated(CategoryTranslationsTable).findById(catId)
            withTranslations shouldNotBe null
        }
    }

    @Test
    fun `foreign key with ref`() {
        transaction(db) {
            val directorId = DirectorTable.repo.createReturning(Director(name = "Alfred")).id
            val categoryId = CategoryTable.repo.createReturning(Category()).id
            val lv = LanguageTable.repo.insertReturning(Language("lv"))
            println(
                CategoryTranslationsTable.repo.createWithRelated(
                    CategoryTranslations(
                        categoryId,
                        languageCode = lv.code,
                        "Latviski"
                    ),
                )
            )
            MovieTable.repo.create(Movie(id = -1, "The Birds", kotlin.time.Clock.System.now(), null, directorId, categoryId))

            val movieWithDirector = MovieTable.repo.withRelated(DirectorTable).selectAll().first()
            movieWithDirector.director?.name shouldBe "Alfred"
            movieWithDirector.title shouldBe "The Birds"
        }
    }

    @Test
    fun `insert with related`() {
        transaction(db) {
            val movieWithDirectorOnly = MovieTable.repo.withRelated(DirectorTable, CategoryTable).createWithRelated(
                movie = Movie(
                    title = "Die Hard",
                    directorId = -1,
                    categoryId = -1,
                    director = Director(name = "John McTiernan"),
                    createdAt = kotlin.time.Clock.System.now(),
                    category = Category()
                )
            )

            movieWithDirectorOnly.directorId shouldNotBe -1
            movieWithDirectorOnly.categoryId shouldNotBe -1
        }
    }

    @Test
    fun `delete entity`() {
        transaction(db) {
            val repo = LanguageTable.repo
            val language = repo.createReturning(Language("lv"))
            repo.findById("lv") shouldBe language
            repo.delete(language)
            repo.findById("lv") shouldBe null
        }
    }

    @Test
    fun `delete entity with composite key`() {
        transaction(db) {
            val repo = CategoryTranslationsTable.repo.withRelated(CategoryTable, LanguageTable)

            val created = repo.createWithRelated(CategoryTranslations(
                -1,
                "lv",
                "Sveiki",
                Category(),
                language = Language("lv")
            ))
            repo.findById(created.categoryId, created.languageCode) shouldNotBe null
            val deletedCount = repo.delete(created)
            deletedCount shouldBe 1
            repo.findById(created.categoryId, created.languageCode) shouldBe null
        }
    }

    @Test
    fun `delete by id`() {
        transaction(db) {
            val repo = CategoryTranslationsTable.repo.withRelated(CategoryTable).withRelated(LanguageTable)
            val created = repo.createWithRelated(
                CategoryTranslations(
                    -1,
                    "",
                    "Sveiki",
                    category = Category(),
                    language = Language("lv")
                )
            )
            repo.findById(created.categoryId, created.languageCode) shouldNotBe null
            repo.deleteById(created.categoryId, created.languageCode)
            repo.findById(created.categoryId, created.languageCode) shouldBe null
        }
    }

    @Test
    fun `unique index`() {
        transaction(db) {
            DirectorTable.repo.apply {
                create(Director(name = "Alfred"))
                shouldThrow<Exception> { create(Director(name = "Alfred")) }
            }
        }
    }


    @Test
    fun `test converted entity with both nullable and non-nullable fields`() {
        transaction(db) {
            val color = Color(255, 23, 7)
            val nullableColor = Color(128, 64, 32)
            val inserted = ConvertedEntityTable.repo.createReturning(
                ConvertedEntity(color = color, nullableColor = nullableColor)
            )
            val found = ConvertedEntityTable.repo.findById(inserted.id)

            found shouldNotBe null
            found?.color shouldBe color
            found?.nullableColor shouldNotBe null
            found?.nullableColor shouldBe nullableColor
        }
    }

    @Test
    fun `test converted entity with null nullable field`() {
        transaction(db) {
            val color = Color(255, 23, 7)
            val inserted = ConvertedEntityTable.repo.createReturning( ConvertedEntity(color = color, nullableColor = null) )
            val found = ConvertedEntityTable.repo.findById(inserted.id)

            found shouldNotBe null
            found?.color shouldBe color
            found?.nullableColor shouldBe null
        }
    }

    @Test
    fun `ignored field entity table only has id and name columns`() {
        transaction(db) {
            val columnNames = IgnoredFieldEntityTable.columns.map { it.name }
            columnNames shouldBe listOf("id", "name")
        }
    }


}
