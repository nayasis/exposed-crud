package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test class for createReturning method with different IdTable types
 * 
 * This tests the logic that uses insertAndGetId for IntIdTable, LongIdTable, 
 * UIntIdTable, ULongIdTable, and UUIDTable when auto-increment is enabled.
 */
class CreateReturningTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:h2:mem:test_create_returning_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MYSQL",
            driver = "org.h2.Driver"
        )
        transaction(db) {
            addLogger(StdOutSqlLogger)
            
            // Drop and create all test tables
            listOf(
                IntIdEntityTable,
                LongIdEntityTable,
                UIntIdEntityTable,
                ULongIdEntityTable,
                UUIDEntityTable
            ).forEach {
                SchemaUtils.drop(it)
                SchemaUtils.create(it)
            }
        }
    }

    @Test
    fun `createReturning with IntIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = IntIdEntity(id = -1, name = "Test Int")
            
            val inserted = IntIdEntityTable.repo.createReturning(entity)
            
            inserted shouldNotBe null
            inserted.id shouldNotBe -1
            inserted.name shouldBe "Test Int"
            
            // Verify we can find it by the returned ID
            val found = IntIdEntityTable.repo.findById(inserted.id)
            found shouldNotBe null
            found?.id shouldBe inserted.id
            found?.name shouldBe inserted.name
        }
    }

    @Test
    fun `createReturning with LongIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = LongIdEntity(id = -1, name = "Test Long")
            
            val inserted = LongIdEntityTable.repo.createReturning(entity)
            
            inserted shouldNotBe null
            inserted.id shouldNotBe -1L
            inserted.name shouldBe "Test Long"
            
            // Verify we can find it by the returned ID
            val found = LongIdEntityTable.repo.findById(inserted.id)
            found shouldNotBe null
            found?.id shouldBe inserted.id
            found?.name shouldBe inserted.name
        }
    }

    @Test
    fun `createReturning with UIntIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = UIntIdEntity(id = 0u, name = "Test UInt")
            
            val inserted = UIntIdEntityTable.repo.createReturning(entity)
            
            inserted shouldNotBe null
            inserted.id shouldNotBe 0u
            inserted.name shouldBe "Test UInt"
            
            // Verify we can find it by the returned ID
            val found = UIntIdEntityTable.repo.findById(inserted.id)
            found shouldNotBe null
            found?.id shouldBe inserted.id
            found?.name shouldBe inserted.name
        }
    }

    @Test
    fun `createReturning with ULongIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = ULongIdEntity(id = 0u, name = "Test ULong")
            
            val inserted = ULongIdEntityTable.repo.createReturning(entity)
            
            inserted shouldNotBe null
            inserted.id shouldNotBe 0u
            inserted.name shouldBe "Test ULong"
            
            // Verify we can find it by the returned ID
            val found = ULongIdEntityTable.repo.findById(inserted.id)
            found shouldNotBe null
            found?.id shouldBe inserted.id
            found?.name shouldBe inserted.name
        }
    }

    @Test
    fun `createReturning multiple entities generates unique IDs`() {
        transaction(db) {
            val entity1 = IntIdEntity(id = -1, name = "Entity 1")
            val entity2 = IntIdEntity(id = -1, name = "Entity 2")
            val entity3 = IntIdEntity(id = -1, name = "Entity 3")
            
            val inserted1 = IntIdEntityTable.repo.createReturning(entity1)
            val inserted2 = IntIdEntityTable.repo.createReturning(entity2)
            val inserted3 = IntIdEntityTable.repo.createReturning(entity3)
            
            // All IDs should be different
            inserted1.id shouldNotBe inserted2.id
            inserted2.id shouldNotBe inserted3.id
            inserted1.id shouldNotBe inserted3.id
            
            // All should be retrievable
            IntIdEntityTable.repo.findById(inserted1.id) shouldNotBe null
            IntIdEntityTable.repo.findById(inserted2.id) shouldNotBe null
            IntIdEntityTable.repo.findById(inserted3.id) shouldNotBe null
        }
    }

    @Test
    fun `createReturning with LongIdTable multiple inserts generates sequential IDs`() {
        transaction(db) {
            val entity1 = LongIdEntity(id = -1, name = "Long Entity 1")
            val entity2 = LongIdEntity(id = -1, name = "Long Entity 2")
            
            val inserted1 = LongIdEntityTable.repo.createReturning(entity1)
            val inserted2 = LongIdEntityTable.repo.createReturning(entity2)
            
            inserted1.id shouldNotBe inserted2.id
            inserted1.name shouldBe "Long Entity 1"
            inserted2.name shouldBe "Long Entity 2"
        }
    }

    @Test
    fun `createReturning with UUIDTable returns inserted entity with generated ID`() {
        transaction(db) {
            val tempId = UUID.randomUUID()
            val entity = UUIDEntity(id = tempId, name = "Test UUID")
            
            val inserted = UUIDEntityTable.repo.createReturning(entity)
            
            inserted shouldNotBe null
            inserted.id shouldNotBe null
            inserted.name shouldBe "Test UUID"
            
            // Verify we can find it by the returned ID
            val found = UUIDEntityTable.repo.findById(inserted.id)
            found shouldNotBe null
            found?.id shouldBe inserted.id
            found?.name shouldBe inserted.name
        }
    }

    @Test
    fun `createReturning with UUIDTable multiple inserts generates unique IDs`() {
        transaction(db) {
            val entity1 = UUIDEntity(id = UUID.randomUUID(), name = "UUID Entity 1")
            val entity2 = UUIDEntity(id = UUID.randomUUID(), name = "UUID Entity 2")
            
            val inserted1 = UUIDEntityTable.repo.createReturning(entity1)
            val inserted2 = UUIDEntityTable.repo.createReturning(entity2)
            
            // All IDs should be different
            inserted1.id shouldNotBe inserted2.id
            inserted1.name shouldBe "UUID Entity 1"
            inserted2.name shouldBe "UUID Entity 2"
        }
    }
}


