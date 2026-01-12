package com.dshatz.exposed_crud.override.name

import com.dshatz.exposed_crud.helper.TestHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.Column
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

class OverrideNameTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(listOf(Users, CustomIdTable))
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
    val pii: Column<EntityID<Int>> = integer("pii").entityId()
    val name = varchar("name", length = 50)
    
    // Map id property to pii (implements IdTable's abstract property)
    override val id: Column<EntityID<Int>> = pii
    
    override val primaryKey = PrimaryKey(pii)
}