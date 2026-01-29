package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.helper.TestHelper
import com.dshatz.exposed_crud.models.EmployeeIndia
import com.dshatz.exposed_crud.models.EmployeeIndiaTable
import com.dshatz.exposed_crud.models.EmployeeJapan
import com.dshatz.exposed_crud.models.EmployeeJapanTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class InheritanceTableTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        // Create only EmployeeJapan and EmployeeIndia tables (DefaultEntity and AbstractEmployee are not tables)
        db = TestHelper.prepareDatabase(
            listOf(
                EmployeeJapanTable,
                EmployeeIndiaTable,
            ),
        )
        
        transaction(db) {
            println("Created tables:")
            SchemaUtils.listTables().forEach { println("  - $it") }
        }
    }

    @Test
    fun `EmployeeJapan and EmployeeIndia should be different tables`() {
        transaction(db) {

            // Insert data into each table
            EmployeeJapanTable.repo.create(EmployeeJapan().apply {
                name = "Tanaka"
                age = 30
            })
            EmployeeIndiaTable.repo.create(EmployeeIndia().apply {
                name = "PatelA"
                age = 28
            })
            EmployeeIndiaTable.repo.create(EmployeeIndia().apply {
                name = "PatelB"
                age = 45
            })

            // Check the number of data in each table
            EmployeeJapanTable.repo.select().count() shouldBe 1
            EmployeeIndiaTable.repo.select().count() shouldBe 2

            val employeeJapan = EmployeeJapanTable.repo.findById(1)
            val employeeIndia = EmployeeIndiaTable.repo.findById(2)

            // Verify that each table has independent data
            employeeJapan?.name shouldBe "Tanaka"
            employeeJapan?.age  shouldBe 30
            employeeIndia?.name shouldBe "PatelB"
            employeeIndia?.age  shouldBe 45
        }
    }

}

