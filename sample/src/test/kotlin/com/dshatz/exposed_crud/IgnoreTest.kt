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
            
            // verify non-ignored fields should be present
            val columnNames = columns.map { it.name }
            columnNames.size shouldBe 3 // id, name, active
            columnNames.contains("id") shouldBe true
            columnNames.contains("name") shouldBe true
            columnNames.contains("active") shouldBe true
            
            // verify ignored fields are not present
            columnNames.contains("ignoredField") shouldBe false
            columnNames.contains("computedProperty") shouldBe false
            columnNames.contains("anotherIgnored") shouldBe false
        }
    }

    @Test
    fun entityWithIgnoredFieldsShouldWork() {
        transaction(db) {

            val data = IgnoreEntity(
                name = "Test Entity",
                active = true,
                ignoredField = "This should be ignored"
            ).apply {
                anotherIgnored = 42
                computedProperty = "This should also be ignored"
            }
            
            val inserted = IgnoreEntityTable.repo.create(data)
            val found = IgnoreEntityTable.repo.findById(inserted.id)

            found shouldNotBe null
            found?.id shouldBe inserted.id
            found?.name shouldBe inserted.name
            found?.active shouldBe inserted.active
            found?.ignoredField shouldBe null
            found?.anotherIgnored shouldBe 0

        }
    }
}

