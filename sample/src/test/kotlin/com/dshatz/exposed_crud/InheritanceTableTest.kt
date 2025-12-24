package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.EmployeeIndia
import com.dshatz.exposed_crud.models.EmployeeIndiaTable
import com.dshatz.exposed_crud.models.EmployeeJapan
import com.dshatz.exposed_crud.models.EmployeeJapanTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class InheritanceTableTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:h2:mem:inheritance_test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "org.h2.Driver"
        )
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // Create only EmployeeJapan and EmployeeIndia tables (DefaultEntity and AbstractEmployee are not tables)
            listOf(
                EmployeeJapanTable,
                EmployeeIndiaTable,
            ).forEach {
                SchemaUtils.create(it)
            }
        }
        
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

