package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.TimestampEntity
import com.dshatz.exposed_crud.models.TimestampEntityTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class TimestampTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:h2:mem:timestamp_test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "org.h2.Driver"
        )
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(TimestampEntityTable)
        }
    }

    @Test
    fun `CreationTimestamp should be set automatically on insert`() {
        transaction(db) {
            val entity = TimestampEntityTable.repo.createReturning(TimestampEntity())
            
            entity.createdAt shouldNotBe null
            entity.tickedAt shouldNotBe null
            entity.updatedAt shouldBe null
        }
    }

    @Test
    fun `UpdateTimestamp should be set automatically on update`() {
        transaction(db) {
            // Insert entity
            val entity = TimestampEntityTable.repo.createReturning(TimestampEntity().apply {
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
            val entity1 = TimestampEntityTable.repo.createReturning(TimestampEntity().apply {
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
            val entity1 = TimestampEntityTable.repo.createReturning(TimestampEntity().apply {
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
}
