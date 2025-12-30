package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.IgnoreEntity
import com.dshatz.exposed_crud.models.IgnoreEntityTable
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

class IgnoreTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:h2:mem:ignore_test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "org.h2.Driver"
        )
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(IgnoreEntityTable)
        }
    }

    @Test
    fun ignoredFieldsShouldNotBeInTable() {
        transaction(db) {
            val columns = IgnoreEntityTable.columns
            
            // Verify table exists
            val tables = SchemaUtils.listTables()
            tables.any { it.contains("IGNORE_ENTITIES", ignoreCase = true) } shouldBe true
            
            // Verify ignored fields are not in the table
            // Only id, name, and active should be present
            val columnNames = columns.map { it.name }
            columnNames.size shouldBe 3 // id, name, active
            columnNames.contains("id") shouldBe true
            columnNames.contains("name") shouldBe true
            columnNames.contains("active") shouldBe true
            
            // Verify ignored fields are not present
            columnNames.contains("ignoredField") shouldBe false
            columnNames.contains("computedProperty") shouldBe false
            columnNames.contains("anotherIgnored") shouldBe false
        }
    }

    @Test
    fun entityWithIgnoredFieldsShouldWork() {
        transaction(db) {
            // Create entity with ignored fields
            val originalEntity = IgnoreEntity(
                name = "Test Entity",
                active = true,
                ignoredField = "This should be ignored"
            ).apply {
                anotherIgnored = 42
                computedProperty = "This should also be ignored"
            }
            
            val entity = IgnoreEntityTable.repo.createReturning(originalEntity)
            
            // Verify entity was created with correct database fields
            entity.id shouldNotBe -1L
            entity.name shouldBe "Test Entity"
            entity.active shouldBe true
            
            // Verify ignored fields are not stored in database
            // createReturning fetches from database, so ignored fields will have default values
            entity.ignoredField shouldBe null
            entity.anotherIgnored shouldBe 0
            
            // Retrieve from database again - same result
            val retrieved = IgnoreEntityTable.repo.findById(entity.id)
            retrieved?.name shouldBe "Test Entity"
            retrieved?.active shouldBe true
            retrieved?.ignoredField shouldBe null
            retrieved?.anotherIgnored shouldBe 0
        }
    }
}

