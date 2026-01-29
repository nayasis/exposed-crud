package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.helper.TestHelper
import com.dshatz.exposed_crud.models.ConverterNullabilityTestEntity
import com.dshatz.exposed_crud.models.ConverterNullabilityTestEntityTable
import com.dshatz.exposed_crud.models.TestValue
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests all 4 nullability combinations for converter transform code generation:
 * 1. Entity nullable, DB nullable
 * 2. Entity nullable, DB non-nullable
 * 3. Entity non-nullable, DB nullable
 * 4. Entity non-nullable, DB non-nullable
 */
class ConverterNullabilityTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(ConverterNullabilityTestEntityTable),
        )
    }

    @Test
    fun `test case 1 - Entity nullable, DB nullable`() {
        transaction(db) {
            val testValue = TestValue("case1-value")
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case1NullableEntityNullableDb = testValue
                )
            )

            entity.case1NullableEntityNullableDb shouldBe testValue

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case1NullableEntityNullableDb shouldBe testValue
        }
    }

    @Test
    fun `test case 1 - Entity nullable, DB nullable with null value`() {
        transaction(db) {
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case1NullableEntityNullableDb = null
                )
            )

            entity.case1NullableEntityNullableDb shouldBe null

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case1NullableEntityNullableDb shouldBe null
        }
    }

    @Test
    fun `test case 2 - Entity nullable, DB non-nullable with value`() {
        transaction(db) {
            val testValue = TestValue("case2-value")
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case2NullableEntityNonNullableDb = testValue
                )
            )

            entity.case2NullableEntityNonNullableDb shouldBe testValue

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case2NullableEntityNonNullableDb shouldBe testValue
        }
    }

    @Test
    fun `test case 2 - Entity nullable, DB non-nullable with null value`() {
        transaction(db) {
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case2NullableEntityNonNullableDb = null
                )
            )

            entity.case2NullableEntityNonNullableDb shouldBe null

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case2NullableEntityNonNullableDb shouldBe null
        }
    }

    @Test
    fun `test case 3 - Entity non-nullable, DB nullable with value`() {
        transaction(db) {
            val testValue = TestValue("case3-value")
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case3NonNullableEntityNullableDb = testValue
                )
            )

            entity.case3NonNullableEntityNullableDb shouldBe testValue

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case3NonNullableEntityNullableDb shouldBe testValue
        }
    }

    @Test
    fun `test case 3 - Entity non-nullable, DB nullable with NULL in DB`() {
        transaction(db) {
            // Insert a row with NULL for case3 column using direct insert
            ConverterNullabilityTestEntityTable.insert {
                // Set required non-null columns
                it[case4NonNullableEntityNonNullableDb] = TestValue("case4")
                // Don't set case3NonNullableEntityNullableDb, leaving it NULL in DB
                // ID will be auto-generated
            }

            // Get the inserted row using repo (which calls toEntity())
            val all = ConverterNullabilityTestEntityTable.repo.selectAll()
            all.size shouldBe 1

            val found = all.first()
            // toEntity() should handle NULL and call converter to get default value
            found.case3NonNullableEntityNullableDb shouldBe TestValue("default")
        }
    }

    @Test
    fun `test case 4 - Entity non-nullable, DB non-nullable with value`() {
        transaction(db) {
            val testValue = TestValue("case4-value")
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case4NonNullableEntityNonNullableDb = testValue
                )
            )

            entity.case4NonNullableEntityNonNullableDb shouldBe testValue

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case4NonNullableEntityNonNullableDb shouldBe testValue
        }
    }

    @Test
    fun `test all cases together`() {
        transaction(db) {
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case1NullableEntityNullableDb = TestValue("value1"),
                    case2NullableEntityNonNullableDb = TestValue("value2"),
                    case3NonNullableEntityNullableDb = TestValue("value3"),
                    case4NonNullableEntityNonNullableDb = TestValue("value4")
                )
            )

            entity.case1NullableEntityNullableDb shouldBe TestValue("value1")
            entity.case2NullableEntityNonNullableDb shouldBe TestValue("value2")
            entity.case3NonNullableEntityNullableDb shouldBe TestValue("value3")
            entity.case4NonNullableEntityNonNullableDb shouldBe TestValue("value4")

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case1NullableEntityNullableDb shouldBe TestValue("value1")
            found?.case2NullableEntityNonNullableDb shouldBe TestValue("value2")
            found?.case3NonNullableEntityNullableDb shouldBe TestValue("value3")
            found?.case4NonNullableEntityNonNullableDb shouldBe TestValue("value4")
        }
    }

    @Test
    fun `test update operations for all cases`() {
        transaction(db) {
            val entity = ConverterNullabilityTestEntityTable.repo.create(
                ConverterNullabilityTestEntity(
                    case1NullableEntityNullableDb = TestValue("initial1"),
                    case2NullableEntityNonNullableDb = TestValue("initial2"),
                    case3NonNullableEntityNullableDb = TestValue("initial3"),
                    case4NonNullableEntityNonNullableDb = TestValue("initial4")
                )
            )

            val updated = entity.copy(
                case1NullableEntityNullableDb = TestValue("updated1"),
                case2NullableEntityNonNullableDb = TestValue("updated2"),
                case3NonNullableEntityNullableDb = TestValue("updated3"),
                case4NonNullableEntityNonNullableDb = TestValue("updated4")
            )

            ConverterNullabilityTestEntityTable.repo.update(updated)

            val found = ConverterNullabilityTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.case1NullableEntityNullableDb shouldBe TestValue("updated1")
            found?.case2NullableEntityNonNullableDb shouldBe TestValue("updated2")
            found?.case3NonNullableEntityNullableDb shouldBe TestValue("updated3")
            found?.case4NonNullableEntityNonNullableDb shouldBe TestValue("updated4")
        }
    }
}
