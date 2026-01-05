package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.TestData
import com.dshatz.exposed_crud.models.TextColumnWithConverterEntity
import com.dshatz.exposed_crud.models.TextColumnWithConverterEntityTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class TextColumnWithConverterTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:h2:mem:text_column_converter_test_${java.util.UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "org.h2.Driver"
        )
        transaction(db) {
            SchemaUtils.drop(TextColumnWithConverterEntityTable)
            SchemaUtils.create(TextColumnWithConverterEntityTable)
        }
    }

    @Test
    fun `test LargeText with Converter`() {
        transaction(db) {
            val entity = TextColumnWithConverterEntityTable.repo.create(
                TextColumnWithConverterEntity(
                    largeTextField = TestData("Large text"),
                    mediumTextField = TestData("Medium text"),
                    textField = TestData("Text"),
                    varcharField = TestData("Varchar")
                )
            )

            entity.id shouldBe 1L
            entity.largeTextField?.value shouldBe "Large text"
            entity.mediumTextField?.value shouldBe "Medium text"
            entity.textField?.value shouldBe "Text"
            entity.varcharField?.value shouldBe "Varchar"

            // 조회 테스트
            val found = TextColumnWithConverterEntityTable.repo.findById(entity.id)
            found shouldBe entity
        }
    }

    @Test
    fun `test nullable fields with null values`() {
        transaction(db) {
            val entity = TextColumnWithConverterEntityTable.repo.create(
                TextColumnWithConverterEntity(
                    largeTextField = null,
                    mediumTextField = null,
                    textField = null,
                    varcharField = null
                )
            )

            entity.largeTextField shouldBe null
            entity.mediumTextField shouldBe null
            entity.textField shouldBe null
            entity.varcharField shouldBe null

            // 조회 테스트
            val found = TextColumnWithConverterEntityTable.repo.findById(entity.id)
            found shouldBe entity
        }
    }

    @Test
    fun `test update with text column annotations and converter`() {
        transaction(db) {
            val entity = TextColumnWithConverterEntityTable.repo.create(
                TextColumnWithConverterEntity(
                    largeTextField = TestData("Original large"),
                    mediumTextField = null,
                    textField = null,
                    varcharField = null
                )
            )

            val updated = entity.copy(
                largeTextField = TestData("Updated large"),
                mediumTextField = TestData("Updated medium"),
                textField = TestData("Updated text"),
                varcharField = TestData("Updated varchar")
            )

            TextColumnWithConverterEntityTable.repo.update(updated)

            val found = TextColumnWithConverterEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found!!.largeTextField?.value shouldBe "Updated large"
            found.mediumTextField?.value shouldBe "Updated medium"
            found.textField?.value shouldBe "Updated text"
            found.varcharField?.value shouldBe "Updated varchar"
        }
    }

    @Test
    fun `test all text column types are created correctly`() {
        transaction(db) {
            // 테이블이 정상적으로 생성되었는지 확인
            val columns = TextColumnWithConverterEntityTable.columns
            val columnNames = columns.map { it.name }.toSet()

            columnNames shouldBe setOf(
                "id",
                "largeTextField",
                "mediumTextField",
                "textField",
                "varcharField"
            )
        }
    }
}

