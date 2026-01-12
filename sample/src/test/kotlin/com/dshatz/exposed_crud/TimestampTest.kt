package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.StringIdTimestampEntity
import com.dshatz.exposed_crud.models.StringIdTimestampEntityTable
import com.dshatz.exposed_crud.models.TimestampEntity
import com.dshatz.exposed_crud.models.TimestampEntityTable
import com.dshatz.exposed_crud.models.repo
import com.dshatz.exposed_crud.helper.TestHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class TimestampTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(
                TimestampEntityTable,
                StringIdTimestampEntityTable
            ),
            url = "jdbc:h2:mem:timestamp_test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY"
        )
    }

    @Test
    fun `CreationTimestamp should be set automatically on insert`() {
        transaction(db) {

            val first = TimestampEntityTable.repo.create(TimestampEntity())
            
            first.createdAt shouldNotBe null
            first.updatedAt shouldBe null
            first.tickedAt shouldNotBe null

            Thread.sleep(100)

            val second = TimestampEntityTable.repo.update(first.copy())

            second.createdAt shouldBe first.createdAt
            second.updatedAt shouldNotBe null
            second.updatedAt shouldNotBe first.updatedAt
            second.tickedAt shouldNotBe null
            second.tickedAt shouldNotBe first.tickedAt

        }
    }

    @Test
    fun `UpdateTimestamp should be set automatically on update`() {
        transaction(db) {
            // Insert entity
            val entity = TimestampEntityTable.repo.create(TimestampEntity().apply {
                name = "Test Entity"
            })
            
            val originalCreatedAt = entity.createdAt
            
            // Wait a bit to ensure timestamp difference
            Thread.sleep(10)
            
            // Update entity
            TimestampEntityTable.repo.update(entity.apply { name = "Updated Name"})

            val updatedEntity = TimestampEntityTable.repo.findById(entity.id)

            // createdAt should remain unchanged
            updatedEntity?.createdAt shouldBe originalCreatedAt
            updatedEntity?.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `both timestamps should work together`() {
        transaction(db) {
            // Insert
            val entity1 = TimestampEntityTable.repo.create(TimestampEntity().apply {
                name = "Entity 1"
            })
            
            entity1.createdAt shouldNotBe null
            entity1.updatedAt shouldBe null
            
            // Wait a bit
            Thread.sleep(10)
            
            // Update
            TimestampEntityTable.repo.update(entity1.apply{name = "Entity 1 Updated"})

            val entity2 = TimestampEntityTable.repo.findById(entity1.id)
            
            entity2?.createdAt shouldBe entity1.createdAt
            entity2?.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `tickedAt should be set on both insert and update`() {
        transaction(db) {
            // Insert - tickedAt should be set (has both @CreationTimestamp and @UpdateTimestamp)
            val entity1 = TimestampEntityTable.repo.create(TimestampEntity().apply {
                name = "Entity with tickedAt"
            })
            
            entity1.tickedAt shouldNotBe null
            val originalTickedAt = entity1.tickedAt
            
            // Wait a bit to ensure timestamp difference
            Thread.sleep(100)
            
            // Update - tickedAt should be updated again
            TimestampEntityTable.repo.update(entity1.apply{name = "Updated Entity"})

            val entity2 = TimestampEntityTable.repo.findById(entity1.id)
            
            // tickedAt should be set on update (may be same date if within same day, but should be set)
            entity2?.tickedAt shouldNotBe null
            entity2?.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `CreationTimestamp should be set automatically on insert for non-auto-incrementing ID entity`() {
        transaction(db) {
            val first = StringIdTimestampEntityTable.repo.create(StringIdTimestampEntity(
                code = "test-001",
                name = "Test Entity"
            ))
            
            first.createdAt shouldNotBe null
            first.updatedAt shouldBe null
            first.tickedAt shouldNotBe null

            Thread.sleep(100)

            val second = StringIdTimestampEntityTable.repo.update(first.copy(name = "Updated Name"))

            second.createdAt shouldBe first.createdAt
            second.updatedAt shouldNotBe null
            second.updatedAt shouldNotBe first.updatedAt
            second.tickedAt shouldNotBe null
            second.tickedAt shouldNotBe first.tickedAt
        }
    }

    @Test
    fun `UpdateTimestamp should be set automatically on update for non-auto-incrementing ID entity`() {
        transaction(db) {
            // Insert entity
            val entity = StringIdTimestampEntityTable.repo.create(StringIdTimestampEntity(
                code = "test-002",
                name = "Test Entity"
            ))
            
            val originalCreatedAt = entity.createdAt
            
            // Wait a bit to ensure timestamp difference
            Thread.sleep(10)
            
            // Update entity
            StringIdTimestampEntityTable.repo.update(entity.copy(name = "Updated Name"))

            val updatedEntity = StringIdTimestampEntityTable.repo.findById("test-002")

            // createdAt should remain unchanged
            updatedEntity?.createdAt shouldBe originalCreatedAt
            updatedEntity?.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `both timestamps should work together for non-auto-incrementing ID entity`() {
        transaction(db) {
            // Insert
            val entity1 = StringIdTimestampEntityTable.repo.create(StringIdTimestampEntity(
                code = "test-003",
                name = "Entity 1"
            ))
            
            entity1.createdAt shouldNotBe null
            entity1.updatedAt shouldBe null
            
            // Wait a bit
            Thread.sleep(10)
            
            // Update
            StringIdTimestampEntityTable.repo.update(entity1.copy(name = "Entity 1 Updated"))

            val entity2 = StringIdTimestampEntityTable.repo.findById("test-003")
            
            entity2?.createdAt shouldBe entity1.createdAt
            entity2?.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `tickedAt should be set on both insert and update for non-auto-incrementing ID entity`() {
        transaction(db) {
            // Insert - tickedAt should be set (has both @CreationTimestamp and @UpdateTimestamp)
            val entity1 = StringIdTimestampEntityTable.repo.create(StringIdTimestampEntity(
                code = "test-004",
                name = "Entity with tickedAt"
            ))
            
            entity1.tickedAt shouldNotBe null
            val originalTickedAt = entity1.tickedAt
            
            // Wait a bit to ensure timestamp difference
            Thread.sleep(100)
            
            // Update - tickedAt should be updated again
            StringIdTimestampEntityTable.repo.update(entity1.copy(name = "Updated Entity"))

            val entity2 = StringIdTimestampEntityTable.repo.findById("test-004")
            
            // tickedAt should be set on update (may be same date if within same day, but should be set)
            entity2?.tickedAt shouldNotBe null
            entity2?.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `timestamp behavior should be identical for auto-incrementing and non-auto-incrementing entities`() {
        transaction(db) {
            // Create entities with same initial state
            val autoEntity = TimestampEntityTable.repo.create(TimestampEntity().apply {
                name = "Auto Entity"
            })
            
            val nonAutoEntity = StringIdTimestampEntityTable.repo.create(StringIdTimestampEntity(
                code = "non-auto-001",
                name = "Non-Auto Entity"
            ))
            
            // Both should have createdAt set, but not updatedAt
            autoEntity.createdAt shouldNotBe null
            autoEntity.updatedAt shouldBe null
            autoEntity.tickedAt shouldNotBe null
            
            nonAutoEntity.createdAt shouldNotBe null
            nonAutoEntity.updatedAt shouldBe null
            nonAutoEntity.tickedAt shouldNotBe null
            
            Thread.sleep(10)
            
            // Update both
            TimestampEntityTable.repo.update(autoEntity.copy(name = "Updated Auto"))
            StringIdTimestampEntityTable.repo.update(nonAutoEntity.copy(name = "Updated Non-Auto"))
            
            val updatedAuto = TimestampEntityTable.repo.findById(autoEntity.id)
            val updatedNonAuto = StringIdTimestampEntityTable.repo.findById("non-auto-001")
            
            // Both should behave identically
            updatedAuto?.createdAt shouldBe autoEntity.createdAt
            updatedAuto?.updatedAt shouldNotBe null
            updatedAuto?.updatedAt shouldNotBe autoEntity.updatedAt
            
            updatedNonAuto?.createdAt shouldBe nonAutoEntity.createdAt
            updatedNonAuto?.updatedAt shouldNotBe null
            updatedNonAuto?.updatedAt shouldNotBe nonAutoEntity.updatedAt
        }
    }
}
