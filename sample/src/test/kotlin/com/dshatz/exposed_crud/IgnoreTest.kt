package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.helper.TestHelper
import com.dshatz.exposed_crud.models.IgnoreEntity
import com.dshatz.exposed_crud.models.IgnoreEntityTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class IgnoreTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(IgnoreEntityTable),
        )
    }

    @Test
    fun ignoredFieldsShouldNotBeInTable() {
        transaction(db) {
            val columns = IgnoreEntityTable.columns
            
            // Verify table exists
            val tables = SchemaUtils.listTables()
            tables.any { it.contains("IGNORE_ENTITIES", ignoreCase = true) } shouldBe true
            
            // verify @Column fields should be present
            val columnNames = columns.map { it.name }
            columnNames.size shouldBe 4 // id, name, ignoredField, active
            columnNames.contains("id") shouldBe true
            columnNames.contains("name") shouldBe true
            columnNames.contains("ignoredField") shouldBe true
            columnNames.contains("active") shouldBe true
            
            // verify non-column fields are not present
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
            found?.ignoredField shouldBe inserted.ignoredField
            found?.anotherIgnored shouldBe 0

        }
    }
}

