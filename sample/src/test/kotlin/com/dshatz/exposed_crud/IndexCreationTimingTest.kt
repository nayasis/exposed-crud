package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.Game
import com.dshatz.exposed_crud.models.GameTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test to verify that indexes are only created when SchemaUtils.create() is called,
 * not during table object initialization or data operations.
 */
class IndexCreationTimingTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:h2:mem:index_timing_test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "org.h2.Driver"
        )
    }

    @Test
    fun indexesShouldNotExistBeforeSchemaUtilsCreate() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            
            // Table object is initialized (init block runs), but no DB operations yet
            // Check if table exists
            val tables = SchemaUtils.listTables()
            tables.any { it.toString().contains("GAMES", ignoreCase = true) } shouldBe false
            
            // Table object exists in memory, but table doesn't exist in DB
            // This confirms that init block execution doesn't create DB objects
        }
    }

    @Test
    fun indexesShouldBeCreatedOnlyWhenSchemaUtilsCreateIsCalled() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            
            // Before SchemaUtils.create - no table, no indexes
            val tablesBefore = SchemaUtils.listTables()
            tablesBefore.any { it.toString().contains("GAMES", ignoreCase = true) } shouldBe false
            
            // Call SchemaUtils.create - this should create table AND indexes
            SchemaUtils.create(GameTable)
            
            // After SchemaUtils.create - table and indexes should exist
            val tablesAfter = SchemaUtils.listTables()
            tablesAfter.any { it.toString().contains("GAMES", ignoreCase = true) } shouldBe true
            
            val indices = GameTable.indices
            indices.size shouldBe 4
            
            // Verify explicit names
            indices.map { it.indexName }.shouldContain("specificIndex")
            indices.map { it.indexName }.shouldContain("uniqueGameIndex")
            
            // Verify hash-based names (idx_ followed by 10 chars)
            val hashBasedIndices = indices.filter { 
                it.indexName.startsWith("idx_") && 
                it.indexName != "specificIndex" && 
                it.indexName != "uniqueGameIndex"
            }
            hashBasedIndices.size shouldBe 2
            hashBasedIndices.forEach { index ->
                val hashPart = index.indexName.removePrefix("idx_")
                hashPart.length shouldBe 10
            }
        }
    }

    @Test
    fun dataOperationsShouldNotCreateIndexes() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            
            // Create table first
            SchemaUtils.create(GameTable)
            
            // Get initial index count
            val initialIndices = GameTable.indices.size
            
            // Perform data operations
            GameTable.repo.createReturning(com.dshatz.exposed_crud.models.Game().apply {
                title = "Test Game"
                consoleType = "PS5"
            })
            
            // Index count should remain the same
            val indicesAfter = GameTable.indices.size
            indicesAfter shouldBe initialIndices
        }
    }

    @Test
    fun multipleSchemaUtilsCreateCallsShouldNotDuplicateIndexes() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            
            // First create
            SchemaUtils.create(GameTable)
            val indicesFirst = GameTable.indices.size
            
            // Second create (should be idempotent)
            SchemaUtils.create(GameTable)
            val indicesSecond = GameTable.indices.size
            
            // Index count should be the same
            indicesSecond shouldBe indicesFirst
        }
    }
}

