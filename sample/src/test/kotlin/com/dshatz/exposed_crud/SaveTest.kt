package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.models.*
import com.dshatz.exposed_crud.helper.TestHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test class for save method
 * 
 * Tests the save function which creates a new entity if ID is empty,
 * otherwise updates the existing entity.
 */
class SaveTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        // Reset sequence for Sample entity
        SampleIdGenerator.seq = 0
        
        db = TestHelper.prepareDatabase(
            listOf(
                LongIdEntityTable,
                IntIdEntityTable,
                UUIDEntityTable,
                SampleTable
            ),
            url = "jdbc:h2:mem:test_save_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MYSQL"
        )
    }

    @AfterTest
    fun cleanup() {
        // Reset sequence after test
        SampleIdGenerator.seq = 0
    }

    @Test
    fun `save with Long ID 0 should create new entity`() {
        transaction(db) {
            val entity = LongIdEntity(id = 0, name = "New Entity")
            
            val saved = LongIdEntityTable.repo.save(entity)
            
            saved.id shouldNotBe 0L
            saved.id shouldBe 1L // Auto-generated ID
            saved.name shouldBe "New Entity"
            
            // Verify it was created, not updated
            val found = LongIdEntityTable.repo.findById(saved.id)
            found shouldNotBe null
            found?.name shouldBe "New Entity"
        }
    }

    @Test
    fun `save with Long ID negative should create new entity`() {
        transaction(db) {
            val entity = LongIdEntity(id = -1, name = "New Entity")
            
            val saved = LongIdEntityTable.repo.save(entity)
            
            // Since -1 <= 0, isIdEmpty should return true and create should be called
            saved.id shouldNotBe -1L
            saved.id shouldBe 1L // Auto-generated ID
            saved.name shouldBe "New Entity"
            
            // Verify it was created in database
            val found = LongIdEntityTable.repo.findById(saved.id)
            found shouldNotBe null
            found?.name shouldBe "New Entity"
        }
    }

    @Test
    fun `save with Long ID positive should update existing entity`() {
        transaction(db) {
            // First create an entity
            val created = LongIdEntityTable.repo.create(LongIdEntity(id = -1, name = "Original"))
            created.id shouldBe 1L
            
            // Then save with updated data
            val updated = LongIdEntityTable.repo.save(created.copy(name = "Updated"))
            
            updated.id shouldBe created.id
            updated.name shouldBe "Updated"
            
            // Verify it was updated in database
            val found = LongIdEntityTable.repo.findById(created.id)
            found shouldNotBe null
            found?.name shouldBe "Updated"
        }
    }

    @Test
    fun `save with Int ID 0 should create new entity`() {
        transaction(db) {
            val entity = IntIdEntity(id = 0, name = "New Int Entity")
            
            val saved = IntIdEntityTable.repo.save(entity)
            
            saved.id shouldNotBe 0
            saved.id shouldBe 1 // Auto-generated ID
            saved.name shouldBe "New Int Entity"
        }
    }

    @Test
    fun `save with Int ID negative should create new entity`() {
        transaction(db) {
            val entity = IntIdEntity(id = -1, name = "New Int Entity")
            
            val saved = IntIdEntityTable.repo.save(entity)
            
            // Since -1 <= 0, isIdEmpty should return true and create should be called
            saved.id shouldNotBe -1
            saved.id shouldBe 1 // Auto-generated ID
            saved.name shouldBe "New Int Entity"
            
            // Verify it was created in database
            val found = IntIdEntityTable.repo.findById(saved.id)
            found shouldNotBe null
            found?.name shouldBe "New Int Entity"
        }
    }

    @Test
    fun `save with Int ID positive should update existing entity`() {
        transaction(db) {
            // First create an entity
            val created = IntIdEntityTable.repo.create(IntIdEntity(id = -1, name = "Original"))
            created.id shouldBe 1
            
            // Then save with updated data
            val updated = IntIdEntityTable.repo.save(created.copy(name = "Updated"))
            
            updated.id shouldBe created.id
            updated.name shouldBe "Updated"
            
            // Verify it was updated in database
            val found = IntIdEntityTable.repo.findById(created.id)
            found shouldNotBe null
            found?.name shouldBe "Updated"
        }
    }

    @Test
    fun `save with String ID empty should create new entity`() {
        transaction(db) {
            val entity = Sample(id = "", name = "New Sample")
            
            val saved = SampleTable.repo.save(entity)
            
            saved.id shouldNotBe ""
            saved.id shouldBe "New Sample_0"
            saved.name shouldBe "New Sample"
        }
    }

    @Test
    fun `save with String ID non-empty should update existing entity`() {
        transaction(db) {
            // First create an entity
            val created = SampleTable.repo.create(Sample(id = "", name = "Original"))
            created.id shouldBe "Original_0"
            
            // Then save with updated data
            val updated = SampleTable.repo.save(created.copy(name = "Updated"))
            
            updated.id shouldBe created.id
            updated.name shouldBe "Updated"
            
            // Verify it was updated in database
            val found = SampleTable.repo.findById(created.id)
            found shouldNotBe null
            found?.name shouldBe "Updated"
        }
    }

    @Test
    fun `save multiple times with empty ID should create multiple entities`() {
        transaction(db) {
            val entity1 = LongIdEntityTable.repo.save(LongIdEntity(id = 0, name = "First"))
            val entity2 = LongIdEntityTable.repo.save(LongIdEntity(id = 0, name = "Second"))
            val entity3 = LongIdEntityTable.repo.save(LongIdEntity(id = 0, name = "Third"))
            
            entity1.id shouldBe 1L
            entity2.id shouldBe 2L
            entity3.id shouldBe 3L
            
            entity1.name shouldBe "First"
            entity2.name shouldBe "Second"
            entity3.name shouldBe "Third"
            
            // All should be retrievable
            LongIdEntityTable.repo.selectAll().size shouldBe 3
        }
    }

    @Test
    fun `save should work correctly with create then update pattern`() {
        transaction(db) {
            // Create using save with empty ID
            val created = LongIdEntityTable.repo.save(LongIdEntity(id = 0, name = "Created"))
            created.id shouldBe 1L
            
            // Update using save with existing ID
            val updated = LongIdEntityTable.repo.save(created.copy(name = "Updated"))
            updated.id shouldBe 1L
            updated.name shouldBe "Updated"
            
            // Verify only one entity exists
            val all = LongIdEntityTable.repo.selectAll()
            all.size shouldBe 1
            all.first().name shouldBe "Updated"
        }
    }

    @Test
    fun `save with non-existent positive ID should attempt update but not create`() {
        transaction(db) {
            // Try to save with a non-existent positive ID
            val entity = LongIdEntity(id = 999, name = "Non-existent")
            
            // Since ID is positive (> 0), it will try to update, but entity doesn't exist
            // Update will return the original object without modifying it
            val saved = LongIdEntityTable.repo.save(entity)
            
            // The entity should be returned as-is (update didn't find anything to update)
            saved.id shouldBe 999L
            saved.name shouldBe "Non-existent"
            
            // Verify it doesn't exist in database (update didn't create it)
            val found = LongIdEntityTable.repo.findById(999L)
            found shouldBe null
        }
    }

    @Test
    fun `save with UUID zero should create new entity`() {
        transaction(db) {
            val entity = UUIDEntity(id = Id.UUID_EMPTY, name = "New UUID Entity")
            
            val saved = UUIDEntityTable.repo.save(entity)
            
            // Zero UUID should trigger create, so ID should be auto-generated (not zero)
            saved.id shouldNotBe Id.UUID_EMPTY
            saved.id.leastSignificantBits shouldNotBe 0L
            saved.id.mostSignificantBits shouldNotBe 0L
            saved.name shouldBe "New UUID Entity"
            
            // Verify it was created in database
            val found = UUIDEntityTable.repo.findById(saved.id)
            found shouldNotBe null
            found?.name shouldBe "New UUID Entity"
        }
    }

    @Test
    fun `save with UUID non-zero should update existing entity`() {
        transaction(db) {
            // First create an entity
            val created = UUIDEntityTable.repo.create(UUIDEntity(id = Id.UUID_EMPTY, name = "Original"))
            created.id shouldNotBe Id.UUID_EMPTY
            
            // Then save with updated data using the existing UUID
            val updated = UUIDEntityTable.repo.save(created.copy(name = "Updated"))
            
            updated.id shouldBe created.id
            updated.name shouldBe "Updated"
            
            // Verify it was updated in database
            val found = UUIDEntityTable.repo.findById(created.id)
            found shouldNotBe null
            found?.name shouldBe "Updated"
        }
    }

    @Test
    fun `save with UUID random value should update existing entity`() {
        transaction(db) {
            // First create an entity
            val created = UUIDEntityTable.repo.create(UUIDEntity(id = Id.UUID_EMPTY, name = "Original"))
            val originalId = created.id
            
            // Then save with a random UUID (non-zero) - should update, not create
            val randomUUID = UUID.randomUUID()
            val updated = UUIDEntityTable.repo.save(created.copy(id = randomUUID, name = "Updated"))
            
            // Since UUID is non-zero, it should try to update
            // But the UUID doesn't exist, so it will return the original object
            updated.id shouldBe randomUUID
            updated.name shouldBe "Updated"
            
            // Verify the original entity still exists with original ID
            val found = UUIDEntityTable.repo.findById(originalId)
            found shouldNotBe null
            found?.name shouldBe "Original"
            
            // The random UUID entity doesn't exist
            val notFound = UUIDEntityTable.repo.findById(randomUUID)
            notFound shouldBe null
        }
    }

    @Test
    fun `saveAll should create multiple entities and set ids`() {
        transaction(db) {
            val items = mutableListOf(
                LongIdEntity(id = 0, name = "First"),
                LongIdEntity(id = 0, name = "Second"),
                LongIdEntity(id = 0, name = "Third")
            )

            LongIdEntityTable.repo.saveAll(items)

            items.map { it.id } shouldBe listOf(1L, 2L, 3L)
            LongIdEntityTable.repo.selectAll().size shouldBe 3
        }
    }

    @Test
    fun `saveAll should update existing and create new entities`() {
        transaction(db) {
            val created = LongIdEntityTable.repo.create(LongIdEntity(id = 0, name = "Original"))
            val updated = created.copy(name = "Updated")
            val newEntity = LongIdEntity(id = 0, name = "New")

            LongIdEntityTable.repo.saveAll(listOf(updated, newEntity))

            val found = LongIdEntityTable.repo.findById(created.id)
            found shouldNotBe null
            found?.name shouldBe "Updated"

            newEntity.id shouldBe 2L
            LongIdEntityTable.repo.selectAll().size shouldBe 2
        }
    }
}
