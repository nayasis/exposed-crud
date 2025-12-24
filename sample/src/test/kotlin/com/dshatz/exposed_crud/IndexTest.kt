package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.GameTable
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class IndexTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:h2:mem:index_test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "org.h2.Driver"
        )
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(GameTable)
        }
        
        transaction(db) {
            println("Created tables:")
            SchemaUtils.listTables().forEach { println("  - $it") }
        }
    }

    @Test
    fun `table should be created with indexes`() {
        transaction(db) {
            // Verify table exists
            val tables = SchemaUtils.listTables()
            tables.any { it.toString().contains("GAMES", ignoreCase = true) } shouldBe true
            
            // Verify indexes exist by checking table indices
            val indices = GameTable.indices
            indices.size shouldBe 2
            
            // Check that our indexes are present
            val indexNames = indices.map { it.indexName }
            indexNames shouldBe listOf("specificIndex", "uniqueGameIndex")
            
            // Verify index columns
            val specificIndex = indices.find { it.indexName == "specificIndex" }
            specificIndex shouldNotBe null
            val uniqueIndex = indices.find { it.indexName == "uniqueGameIndex" }
            uniqueIndex shouldNotBe null
            
            // Print index information for verification
            println("Found ${indices.size} indexes:")
            indices.forEach { index ->
                println("  - ${index.indexName}: ${index.createStatement().first()}")
            }
        }
    }
}
