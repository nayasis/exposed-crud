package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.helper.TestHelper
import com.dshatz.exposed_crud.models.Category
import com.dshatz.exposed_crud.models.CategoryTable
import com.dshatz.exposed_crud.models.CategoryTranslations
import com.dshatz.exposed_crud.models.CategoryTranslationsTable
import com.dshatz.exposed_crud.models.ColumnTestEntity
import com.dshatz.exposed_crud.models.ColumnTestEntityTable
import com.dshatz.exposed_crud.models.Language
import com.dshatz.exposed_crud.models.LanguageTable
import com.dshatz.exposed_crud.models.existsById
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class ExistsByIdTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(
                ColumnTestEntityTable,
                LanguageTable,
                CategoryTable,
                CategoryTranslationsTable
            ),
        )
    }

    @Test
    fun `existsById supports single primary key`() {
        transaction(db) {
            val created = ColumnTestEntityTable.repo.create(
                ColumnTestEntity(
                    defaultColumn = "default",
                    blankColumn = "blank",
                    customColumn = "custom",
                    normalColumn = "normal"
                )
            )

            ColumnTestEntityTable.repo.existsById(created.id) shouldBe true
            ColumnTestEntityTable.repo.existsById(created.id + 1) shouldBe false
        }
    }

    @Test
    fun `existsById supports composite primary key`() {
        transaction(db) {
            val category = CategoryTable.repo.create(Category())
            val language = LanguageTable.repo.create(Language("ko"))
            val created = CategoryTranslationsTable.repo.create(
                CategoryTranslations(
                    categoryId = category.id,
                    languageCode = language.code,
                    translation = "Korean"
                )
            )

            CategoryTranslationsTable.repo.existsById(created.categoryId, created.languageCode) shouldBe true
            CategoryTranslationsTable.repo.existsById(created.categoryId, "en") shouldBe false
        }
    }
}
