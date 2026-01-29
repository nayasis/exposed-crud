package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.ColumnTestEntity
import com.dshatz.exposed_crud.models.ColumnTestEntityTable
import com.dshatz.exposed_crud.models.repo
import com.dshatz.exposed_crud.helper.TestHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class ColumnTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(ColumnTestEntityTable)
        )
    }

    @Test
    fun `test Column annotation with default value and blank handling`() {
        transaction(db) {
            // Verify column names
            val columnNames = ColumnTestEntityTable.columns.map { it.name }.toSet()
            
            // @Column() - uses default value (blank) -> column name generated from property name (defaultColumn -> defaultColumn)
            columnNames shouldBe setOf("id", "defaultColumn", "blankColumn", "custom_name")
            
            // Test entity creation and saving
            val entity = ColumnTestEntityTable.repo.create(
                ColumnTestEntity(
                    defaultColumn = "default",
                    blankColumn = "blank",
                    customColumn = "custom",
                    normalColumn = "normal"
                )
            )
            
            entity.id shouldBe 1L
            entity.defaultColumn shouldBe "default"
            entity.blankColumn shouldBe "blank"
            entity.customColumn shouldBe "custom"
            entity.normalColumn shouldBe "normal"
            
            // Test retrieval
            val found = ColumnTestEntityTable.repo.findById(entity.id)
            found shouldNotBe null
            found?.id shouldBe entity.id
            found?.defaultColumn shouldBe entity.defaultColumn
            found?.blankColumn shouldBe entity.blankColumn
            found?.customColumn shouldBe entity.customColumn
            found?.normalColumn shouldBe ""
        }
    }

    @Test
    fun `test Column annotation blank values are ignored`() {
        transaction(db) {
            // Verify that both @Column() and @Column("") generate column names based on property names
            val columnNames = ColumnTestEntityTable.columns.map { it.name }.toSet()
            
            // @Column() - uses default value (blank) -> column name generated from property name
            columnNames.contains("defaultColumn") shouldBe true
            
            // @Column("") - explicitly empty string -> column name generated from property name
            columnNames.contains("blankColumn") shouldBe true
            
            // @Column("custom_name") - normal name specification -> uses specified name
            columnNames.contains("custom_name") shouldBe true
            
            // No annotation -> not generated as a column
            columnNames.contains("normalColumn") shouldBe false
        }
    }
}

