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
import com.dshatz.exposed_crud.models.Language
import com.dshatz.exposed_crud.models.LanguageTable
import com.dshatz.exposed_crud.models.Movie
import com.dshatz.exposed_crud.models.MovieTable
import com.dshatz.exposed_crud.models.createWithRelated
import com.dshatz.exposed_crud.models.deleteById
import com.dshatz.exposed_crud.models.findById
import com.dshatz.exposed_crud.models.repo
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestDB {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect("jdbc:sqlite:memory:test_db_${java.util.UUID.randomUUID()}?foreign_keys=on", "org.sqlite.JDBC")
        transaction(db) {
            addLogger(StdOutSqlLogger)
            listOf( DirectorTable, MovieTable, LanguageTable, CategoryTable, CategoryTranslationsTable, ConvertedEntityTable ).forEach {
                SchemaUtils.drop(it)
                SchemaUtils.create(it)
            }
        }
        transaction(db) {
            println(SchemaUtils.listTables())
        }
    }

    @Test
    fun `test insert`() = transaction {
        val director = Director(0, "Alfred")
        val inserted = DirectorTable.repo.createReturning(director.copy(oldDirector = director))
        val found = DirectorTable.repo.select().where(DirectorTable.name eq "Alfred").first()
        assertEquals("Alfred", found.name)
        assertEquals(inserted, found)
    }

    @Test
    fun `typed select all`() {
        transaction(db) {
            DirectorTable.insert {
                it[name] = "Alfred"
            }
            val all = DirectorTable.repo.selectAll()
            assertEquals(1, all.size)
            assertEquals("Alfred", all.first().name)
        }
    }

    @Test
    fun `typed insert`() = transaction {
        DirectorTable.repo.create(Director(name = "Bob"))
        assertEquals("Bob", DirectorTable.repo.selectAll().first().name)
    }

    @Test
    fun `update custom where`() = transaction {
        DirectorTable.repo.create(Director(name = "Bob"))
        val id = DirectorTable.repo.selectAll().find { it.name == "Bob" }!!.id
        DirectorTable.repo.update({ DirectorTable.id eq id }, Director(name = "Marley"))

        assertEquals("Marley", DirectorTable.repo.selectAll().first().name)
    }

    @Test
    fun `update by simple primary key`() = transaction {
        val id = DirectorTable.repo.createReturning(Director(name = "Bob")).id
        DirectorTable.repo.update(Director(id, "Marley"))
        assertEquals("Marley", DirectorTable.repo.selectAll().first().name)
    }

    @Test
    fun `select where`() = transaction {
        val id = DirectorTable.repo.createReturning(Director(name = "Bob")).id
        val director = DirectorTable.repo.select().where {
            DirectorTable.name eq "Bob"
        }.first()

        assertEquals(id, director.id)
        assertEquals("Bob", director.name)
    }

    @Test
    fun `find by id`() = transaction {
        val directorId = DirectorTable.repo.createReturning(Director(name = "Alfred")).id

        val found = DirectorTable.repo.findById(directorId)
        assertNotNull(found)
        assertEquals("Alfred", found.name)
    }

    @Test
    fun `composite ids`(): Unit = transaction {
        val lang = "lv"
        LanguageTable.repo.create(Language(lang))
        val catId = CategoryTable.repo.createReturning(Category()).id
        CategoryTranslationsTable.repo.create(
            CategoryTranslations(
                catId, lang, "Latviski"
            )
        )
        val found = CategoryTranslationsTable.repo.findById(catId, lang)
        assertEquals("Latviski", found?.translation)

        val withTranslations = CategoryTable.repo.withRelated(CategoryTranslationsTable).findById(catId)
        assertNotNull(withTranslations)
    }

    @Test
    fun `foreign key with ref`(): Unit = transaction {
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
        MovieTable.repo.create(Movie(id = -1, "The Birds", Clock.System.now(), null, directorId, categoryId))

        val movieWithDirector = MovieTable.repo.withRelated(DirectorTable).selectAll().first()
        assertEquals("Alfred", movieWithDirector.director?.name)
        assertEquals("The Birds", movieWithDirector.title)
    }

    @Test
    fun `insert with related`() = transaction {
        val movieWithDirectorOnly = MovieTable.repo.withRelated(DirectorTable, CategoryTable).createWithRelated(
            movie = Movie(
                title = "Die Hard",
                directorId = -1,
                categoryId = -1,
                director = Director(name = "John McTiernan"),
                createdAt = Clock.System.now(),
                category = Category()
            )
        )

        assertNotEquals(-1, movieWithDirectorOnly.directorId)
        assertNotEquals(-1, movieWithDirectorOnly.categoryId)
    }

    @Test
    fun `back references`(): Unit = transaction {
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
        assertEquals(category, translation?.category)
        assertEquals("lv", translation?.language?.code)

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
        assertNotNull(withTranslations?.translations)
        assertEquals(2, withTranslations.translations.size)
        assertEquals("Latviski", withTranslations.translations.find { it.languageCode == "lv" }?.translation)
        assertEquals("In english", withTranslations.translations.find { it.languageCode == "en" }?.translation)

        val director = DirectorTable.repo.createReturning(Director(name = "Alfred"))
        val movie1 = MovieTable.repo.createReturning(
            Movie(title = "The birds", directorId = director.id, categoryId = category.id, createdAt = Clock.System.now()),
        )

        val movie2 = MovieTable.repo.createReturning(
            Movie(title = "The birds 2", directorId = director.id, categoryId = category.id, createdAt = Clock.System.now()),
        )

        val directorWithMovies = DirectorTable.repo.withRelated(MovieTable).findById(director.id)
        assertNotNull(directorWithMovies?.movies)
        assertEquals(setOf(movie1, movie2), directorWithMovies.movies.toSet())

        val categoryWithMovies = CategoryTable.repo.withRelated(MovieTable).findById(category.id)
        assertNotNull(categoryWithMovies?.movies)
        assertEquals(setOf(movie1, movie2), categoryWithMovies.movies.toSet())
    }

    @Test
    fun `delete entity`() = transaction {
        val repo = LanguageTable.repo
        val language = repo.createReturning(Language("lv"))
        assertEquals(language, repo.findById("lv"))
        repo.delete(language)
        assertNull(repo.findById("lv"))
    }

    @Test
    fun `delete entity with composite key`() = transaction {
        val repo = CategoryTranslationsTable.repo.withRelated(CategoryTable, LanguageTable)

        val created = repo.createWithRelated(CategoryTranslations(
            -1,
            "lv",
            "Sveiki",
            Category(),
            language = Language("lv")
        ))
        assertNotNull(repo.findById(created.categoryId, created.languageCode))
        val deletedCount = repo.delete(created)
        assertEquals(1, deletedCount)
        assertNull(repo.findById(created.categoryId, created.languageCode))
    }

    @Test
    fun `delete by id`() = transaction {
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
        assertNotNull(repo.findById(created.categoryId, created.languageCode))
        repo.deleteById(created.categoryId, created.languageCode)
        assertNull(repo.findById(created.categoryId, created.languageCode))
    }

    @Test
    fun `unique index`(): Unit = transaction {
        DirectorTable.repo.apply {
            create(Director(name = "Alfred"))
            assertFails { create(Director(name = "Alfred")) }
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

            assertNotNull(found)
            assertEquals(color, found.color)
            assertNotNull(found.nullableColor)
            assertEquals(nullableColor, found.nullableColor)
        }
    }

    @Test
    fun `test converted entity with null nullable field`() {
        transaction(db) {
            val color = Color(255, 23, 7)
            val inserted = ConvertedEntityTable.repo.createReturning( ConvertedEntity(color = color, nullableColor = null) )
            val found = ConvertedEntityTable.repo.findById(inserted.id)

            assertNotNull(found)
            assertEquals(color, found.color)
            assertNull(found.nullableColor)
        }
    }


}
