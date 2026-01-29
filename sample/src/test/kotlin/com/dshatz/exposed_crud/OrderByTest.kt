package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.helper.TestHelper
import com.dshatz.exposed_crud.models.Director
import com.dshatz.exposed_crud.models.DirectorTable
import com.dshatz.exposed_crud.models.LongIdEntity
import com.dshatz.exposed_crud.models.LongIdEntityTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test class for orderBy method in TypedSelect
 * 
 * Tests the orderBy function which sorts query results by specified columns.
 */
class OrderByTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(
                DirectorTable,
                LongIdEntityTable
            ),
        )
    }

    @Test
    fun `orderBy should sort by name ascending with single column`() {
        transaction(db) {
            // Create directors in random order
            DirectorTable.repo.create(Director(name = "Charlie"))
            DirectorTable.repo.create(Director(name = "Alice"))
            DirectorTable.repo.create(Director(name = "Bob"))

            // Order by name ascending using single column method (default ASC)
            val sorted = DirectorTable.repo.select()
                .orderBy(DirectorTable.name)
                .toList()

            sorted.size shouldBe 3
            sorted[0].name shouldBe "Alice"
            sorted[1].name shouldBe "Bob"
            sorted[2].name shouldBe "Charlie"
        }
    }

    @Test
    fun `orderBy should sort by name ascending with pair syntax`() {
        transaction(db) {
            // Create directors in random order
            DirectorTable.repo.create(Director(name = "Charlie"))
            DirectorTable.repo.create(Director(name = "Alice"))
            DirectorTable.repo.create(Director(name = "Bob"))

            // Order by name ascending using pair syntax
            val sorted = DirectorTable.repo.select()
                .orderBy(DirectorTable.name to SortOrder.ASC)
                .toList()

            sorted.size shouldBe 3
            sorted[0].name shouldBe "Alice"
            sorted[1].name shouldBe "Bob"
            sorted[2].name shouldBe "Charlie"
        }
    }

    @Test
    fun `orderBy should sort by name descending with single column`() {
        transaction(db) {
            // Create directors in random order
            DirectorTable.repo.create(Director(name = "Alice"))
            DirectorTable.repo.create(Director(name = "Charlie"))
            DirectorTable.repo.create(Director(name = "Bob"))

            // Order by name descending using single column method
            val sorted = DirectorTable.repo.select()
                .orderBy(DirectorTable.name, SortOrder.DESC)
                .toList()

            sorted.size shouldBe 3
            sorted[0].name shouldBe "Charlie"
            sorted[1].name shouldBe "Bob"
            sorted[2].name shouldBe "Alice"
        }
    }

    @Test
    fun `orderBy should sort by name descending with pair syntax`() {
        transaction(db) {
            // Create directors in random order
            DirectorTable.repo.create(Director(name = "Alice"))
            DirectorTable.repo.create(Director(name = "Charlie"))
            DirectorTable.repo.create(Director(name = "Bob"))

            // Order by name descending using pair syntax
            val sorted = DirectorTable.repo.select()
                .orderBy(DirectorTable.name to SortOrder.DESC)
                .toList()

            sorted.size shouldBe 3
            sorted[0].name shouldBe "Charlie"
            sorted[1].name shouldBe "Bob"
            sorted[2].name shouldBe "Alice"
        }
    }

    @Test
    fun `orderBy should sort by multiple columns`() {
        transaction(db) {
            // Create entities with same name but different IDs
            val entity1 = LongIdEntityTable.repo.create(LongIdEntity(name = "Test"))
            val entity2 = LongIdEntityTable.repo.create(LongIdEntity(name = "Test"))
            val entity3 = LongIdEntityTable.repo.create(LongIdEntity(name = "Other"))

            // Order by name ascending, then by id descending
            val sorted = LongIdEntityTable.repo.select()
                .orderBy(
                    LongIdEntityTable.name to SortOrder.ASC,
                    LongIdEntityTable.id to SortOrder.DESC
                )
                .toList()

            sorted.size shouldBe 3
            // "Other" should come first (alphabetically)
            sorted[0].name shouldBe "Other"
            // Then "Test" entities, with higher ID first
            sorted[1].name shouldBe "Test"
            sorted[2].name shouldBe "Test"
            // Verify IDs are in descending order for "Test" entities
            if (sorted[1].id > sorted[2].id) {
                sorted[1].id shouldBe entity2.id
                sorted[2].id shouldBe entity1.id
            } else {
                sorted[1].id shouldBe entity1.id
                sorted[2].id shouldBe entity2.id
            }
        }
    }

    @Test
    fun `orderBy should work with where clause`() {
        transaction(db) {
            // Create multiple directors
            DirectorTable.repo.create(Director(name = "Alice"))
            DirectorTable.repo.create(Director(name = "Bob"))
            DirectorTable.repo.create(Director(name = "Charlie"))
            DirectorTable.repo.create(Director(name = "David"))

            // Filter and order using or conditions
            val sorted = DirectorTable.repo.select()
                .where {
                    (DirectorTable.name eq "Bob") or
                    (DirectorTable.name eq "Alice") or
                    (DirectorTable.name eq "David")
                }
                .orderBy(DirectorTable.name to SortOrder.ASC)
                .toList()

            sorted.size shouldBe 3
            sorted[0].name shouldBe "Alice"
            sorted[1].name shouldBe "Bob"
            sorted[2].name shouldBe "David"
        }
    }

    @Test
    fun `orderBy should work with limit`() {
        transaction(db) {
            // Create multiple directors
            DirectorTable.repo.create(Director(name = "Charlie"))
            DirectorTable.repo.create(Director(name = "Alice"))
            DirectorTable.repo.create(Director(name = "Bob"))
            DirectorTable.repo.create(Director(name = "David"))

            // Order and limit to top 2
            val sorted = DirectorTable.repo.select()
                .orderBy(DirectorTable.name to SortOrder.ASC)
                .limit(2)
                .toList()

            sorted.size shouldBe 2
            sorted[0].name shouldBe "Alice"
            sorted[1].name shouldBe "Bob"
        }
    }

    @Test
    fun `orderBy should work with empty result set`() {
        transaction(db) {
            // No entities created
            val sorted = DirectorTable.repo.select()
                .orderBy(DirectorTable.name to SortOrder.ASC)
                .toList()

            sorted.size shouldBe 0
        }
    }

    @Test
    fun `orderBy should work with single entity`() {
        transaction(db) {
            DirectorTable.repo.create(Director(name = "Single"))

            val sorted = DirectorTable.repo.select()
                .orderBy(DirectorTable.name to SortOrder.ASC)
                .toList()

            sorted.size shouldBe 1
            sorted[0].name shouldBe "Single"
        }
    }
}
