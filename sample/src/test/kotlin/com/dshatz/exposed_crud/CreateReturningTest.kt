package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.IntIdEntity
import com.dshatz.exposed_crud.models.IntIdEntityTable
import com.dshatz.exposed_crud.models.LongIdEntity
import com.dshatz.exposed_crud.models.LongIdEntityTable
import com.dshatz.exposed_crud.models.UIntIdEntity
import com.dshatz.exposed_crud.models.UIntIdEntityTable
import com.dshatz.exposed_crud.models.ULongIdEntity
import com.dshatz.exposed_crud.models.ULongIdEntityTable
import com.dshatz.exposed_crud.models.repo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

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
            ).forEach {
                SchemaUtils.drop(it)
                SchemaUtils.create(it)
            }
        }
    }

    @Test
    fun `createReturning with IntIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = IntIdEntity(name = "Test Int")
            
            val inserted = IntIdEntityTable.repo.createReturning(entity)
            
            assertNotNull(inserted)
            assertNotEquals(-1, inserted.pkId)
            assertEquals("Test Int", inserted.name)
            
            // Verify we can find it by the returned ID
            val found = IntIdEntityTable.repo.findById(inserted.pkId)
            assertNotNull(found)
            assertEquals(inserted.pkId, found.pkId)
            assertEquals(inserted.name, found.name)
        }
    }

    @Test
    fun `createReturning with LongIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = LongIdEntity(name = "Test Long")
            
            val inserted = LongIdEntityTable.repo.createReturning(entity)
            
            assertNotNull(inserted)
            assertNotEquals(-1L, inserted.pkId)
            assertEquals("Test Long", inserted.name)
            
            // Verify we can find it by the returned ID
            val found = LongIdEntityTable.repo.findById(inserted.pkId)
            assertNotNull(found)
            assertEquals(inserted.pkId, found.pkId)
            assertEquals(inserted.name, found.name)
        }
    }

    @Test
    fun `createReturning with UIntIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = UIntIdEntity(name = "Test UInt")
            
            val inserted = UIntIdEntityTable.repo.createReturning(entity)
            
            assertNotNull(inserted)
            assertNotEquals(0u, inserted.pkId)
            assertEquals("Test UInt", inserted.name)
            
            // Verify we can find it by the returned ID
            val found = UIntIdEntityTable.repo.findById(inserted.pkId)
            assertNotNull(found)
            assertEquals(inserted.pkId, found.pkId)
            assertEquals(inserted.name, found.name)
        }
    }

    @Test
    fun `createReturning with ULongIdTable returns inserted entity with generated ID`() {
        transaction(db) {
            val entity = ULongIdEntity(name = "Test ULong")
            
            val inserted = ULongIdEntityTable.repo.createReturning(entity)
            
            assertNotNull(inserted)
            assertNotEquals(0u, inserted.pkId)
            assertEquals("Test ULong", inserted.name)
            
            // Verify we can find it by the returned ID
            val found = ULongIdEntityTable.repo.findById(inserted.pkId)
            assertNotNull(found)
            assertEquals(inserted.pkId, found.pkId)
            assertEquals(inserted.name, found.name)
        }
    }

    @Test
    fun `createReturning multiple entities generates unique IDs`() {
        transaction(db) {
            val entity1 = IntIdEntity(name = "Entity 1")
            val entity2 = IntIdEntity(name = "Entity 2")
            val entity3 = IntIdEntity(name = "Entity 3")
            
            val inserted1 = IntIdEntityTable.repo.createReturning(entity1)
            val inserted2 = IntIdEntityTable.repo.createReturning(entity2)
            val inserted3 = IntIdEntityTable.repo.createReturning(entity3)
            
            // All IDs should be different
            assertNotEquals(inserted1.pkId, inserted2.pkId)
            assertNotEquals(inserted2.pkId, inserted3.pkId)
            assertNotEquals(inserted1.pkId, inserted3.pkId)
            
            // All should be retrievable
            assertNotNull(IntIdEntityTable.repo.findById(inserted1.pkId))
            assertNotNull(IntIdEntityTable.repo.findById(inserted2.pkId))
            assertNotNull(IntIdEntityTable.repo.findById(inserted3.pkId))
        }
    }

    @Test
    fun `createReturning with LongIdTable multiple inserts generates sequential IDs`() {
        transaction(db) {
            val entity1 = LongIdEntity(name = "Long Entity 1")
            val entity2 = LongIdEntity(name = "Long Entity 2")
            
            val inserted1 = LongIdEntityTable.repo.createReturning(entity1)
            val inserted2 = LongIdEntityTable.repo.createReturning(entity2)
            
            assertNotEquals(inserted1.pkId, inserted2.pkId)
            assertEquals("Long Entity 1", inserted1.name)
            assertEquals("Long Entity 2", inserted2.name)
        }
    }

}


