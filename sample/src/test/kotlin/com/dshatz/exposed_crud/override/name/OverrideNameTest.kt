package com.dshatz.exposed_crud.override.name

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.helper.TestHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.Column as ExposedColumn
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Test
import kotlin.test.BeforeTest

/**
 * Custom entity for verifying non-default id column names (pii).
 */
@Entity
data class PiiIdEntity(
    @Id(autoGenerate = true)
    @Column
    var pii: Int = -1,
    @Column
    var name: String
)

class OverrideNameTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(listOf(Users, CustomIdTable, PiiIdEntityTable))
    }

    @Test
    fun `original exposed CRUD`() {
        transaction(db) {
            Users.insert {
                it[userId] = "andrew"
                it[name]   = "Andrew Rolando"
                it[age]    = 20
            }

            val user = Users.selectAll().where { Users.userId eq "andrew" }.firstOrNull()

            user shouldNotBe null
            user?.let { it[Users.name] } shouldBe "Andrew Rolando"
        }
    }

    @Test
    fun `IdTable with custom id field name`() {
        transaction(db) {
            // IdTable with custom id field name pii
            val piiId = CustomIdTable.insertAndGetId {
                it[pii] = 100
                it[name] = "Test User"
            }

            piiId shouldNotBe null
            piiId.value shouldBe 100

            val found = CustomIdTable.selectAll().where { CustomIdTable.pii eq EntityID(100, CustomIdTable) }.firstOrNull()
            found shouldNotBe null
            found?.let { it[CustomIdTable.name] } shouldBe "Test User"
            found?.let { it[CustomIdTable.pii] }?.value shouldBe 100
        }
    }

    @Test
    fun `IdTable id property works correctly`() {
        transaction(db) {
            // Verify that id property references pii
            val piiId = CustomIdTable.insertAndGetId {
                it[pii] = 200
                it[name] = "Another User"
            }

            // Query using id property
            val foundById = CustomIdTable.selectAll().where { CustomIdTable.id eq EntityID(200, CustomIdTable) }.firstOrNull()
            foundById shouldNotBe null
            foundById?.let { it[CustomIdTable.name] } shouldBe "Another User"
        }
    }

    @Test
    fun `Entity with pii as ID should work correctly`() {
        transaction(db) {
            // Create entity with pii as ID
            val entity = PiiIdEntity(pii = -1, name = "Test User")
            val created = PiiIdEntityTable.repo.create(entity)

            // Verify pii was auto-generated
            created.pii shouldNotBe -1
            created.pii shouldBe 1
            created.name shouldBe "Test User"

            // Find by pii
            val found = PiiIdEntityTable.repo.findById(created.pii)
            found shouldNotBe null
            found?.pii shouldBe created.pii
            found?.name shouldBe "Test User"
        }
    }

    @Test
    fun `Entity with pii as ID should support update`() {
        transaction(db) {
            // Create entity
            val entity = PiiIdEntity(pii = -1, name = "Original Name")
            val created = PiiIdEntityTable.repo.create(entity)

            // Update entity
            val updated = PiiIdEntityTable.repo.update(created.copy(name = "Updated Name"))

            updated.pii shouldBe created.pii
            updated.name shouldBe "Updated Name"

            // Verify update in database
            val found = PiiIdEntityTable.repo.findById(created.pii)
            found shouldNotBe null
            found?.name shouldBe "Updated Name"
        }
    }

    @Test
    fun `Entity with pii as ID should support save`() {
        transaction(db) {
            // Save with empty ID should create
            val entity1 = PiiIdEntity(pii = -1, name = "New Entity")
            val saved1 = PiiIdEntityTable.repo.save(entity1)

            saved1.pii shouldNotBe -1
            saved1.pii shouldBe 1
            saved1.name shouldBe "New Entity"

            // Save with existing ID should update
            val saved2 = PiiIdEntityTable.repo.save(saved1.copy(name = "Updated Entity"))
            saved2.pii shouldBe saved1.pii
            saved2.name shouldBe "Updated Entity"

            // Verify update
            val found = PiiIdEntityTable.repo.findById(saved1.pii)
            found?.name shouldBe "Updated Entity"
        }
    }

    @Test
    fun `Entity with pii as ID should support selectAll`() {
        transaction(db) {
            // Create multiple entities
            PiiIdEntityTable.repo.create(PiiIdEntity(pii = -1, name = "First"))
            PiiIdEntityTable.repo.create(PiiIdEntity(pii = -1, name = "Second"))
            PiiIdEntityTable.repo.create(PiiIdEntity(pii = -1, name = "Third"))

            // Select all
            val all = PiiIdEntityTable.repo.selectAll()
            all.size shouldBe 3
            all.map { it.name } shouldBe listOf("First", "Second", "Third")
            all.map { it.pii }.distinct().size shouldBe 3 // All pii values should be unique
        }
    }

    @Test
    fun `Entity with pii as ID should support delete`() {
        transaction(db) {
            // Create entity
            val entity = PiiIdEntityTable.repo.create(PiiIdEntity(pii = -1, name = "To Delete"))

            // Delete entity
            PiiIdEntityTable.repo.deleteById(entity.pii)

            // Verify deletion
            val found = PiiIdEntityTable.repo.findById(entity.pii)
            found shouldBe null
        }
    }

}

object Users : Table() {
    val userId = varchar("id", 10)
    val name = varchar("name", length = 50)
    val age = integer("age")
    override val primaryKey = PrimaryKey(userId, name = "PK_User_ID")
}

// IdTable with custom id field name pii
object CustomIdTable : IdTable<Int>("custom_id_table") {
    // pii is defined as a regular property
    val pii: ExposedColumn<EntityID<Int>> = integer("pii").entityId()
    val name = varchar("name", length = 50)
    
    // Map id property to pii (implements IdTable's abstract property)
    override val id: ExposedColumn<EntityID<Int>> = pii
    
    override val primaryKey = PrimaryKey(pii)
}