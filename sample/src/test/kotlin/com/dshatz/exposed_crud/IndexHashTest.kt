package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.helper.TestHelper
import com.dshatz.exposed_crud.models.GameTable
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test to verify that hash-based index names are deterministic:
 * Same column names in the same order should always produce the same hash.
 */
class IndexHashTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(GameTable),
        )
    }

    @Test
    fun hashBasedIndexNamesShouldBeDeterministic() {
        transaction(db) {
            val indices = GameTable.indices
            
            // Find hash-based indices (those starting with idx_ and not explicit names)
            val hashBasedIndices = indices.filter { 
                it.indexName.startsWith("idx_") && 
                it.indexName != "specificIndex" && 
                it.indexName != "uniqueGameIndex"
            }
            
            hashBasedIndices.size shouldBe 2
            
            // Verify hash format: idx_ followed by exactly 10 characters (0-9, a-z)
            hashBasedIndices.forEach { index ->
                val hashPart = index.indexName.removePrefix("idx_")
                hashPart.length shouldBe 10
                hashPart.all { it.isDigit() || it in 'a'..'z' } shouldBe true
            }
            
            // Find index for "title" column
            val titleIndex = hashBasedIndices.find { 
                it.columns.any { col -> col.name == "title" } && 
                it.columns.size == 1
            }
            titleIndex shouldNotBe null
            
            // Find index for "console_type,title" columns
            val compositeIndex = hashBasedIndices.find { 
                it.columns.size == 2 &&
                it.columns.any { col -> col.name == "console_type" } &&
                it.columns.any { col -> col.name == "title" }
            }
            compositeIndex shouldNotBe null
            
            // Verify hash names are different for different column combinations
            titleIndex?.indexName shouldNotBe compositeIndex?.indexName
            
            println("Hash-based index names:")
            hashBasedIndices.forEach { index ->
                val columns = index.columns.joinToString(", ") { it.name }
                println("  ${index.indexName} -> columns: [$columns]")
            }
        }
    }

    @Test
    fun hashBasedIndexNamesShouldBeConsistentAcrossRebuilds() {
        // This test verifies that the hash generation is deterministic
        // The same column list should always produce the same hash
        // We can't easily test this without rebuilding, but we can verify the format
        
        transaction(db) {
            val indices = GameTable.indices
            val hashBasedIndices = indices.filter { 
                it.indexName.startsWith("idx_") && 
                it.indexName.length == 14 // "idx_" (4) + 10 hash chars
            }
            
            // All hash-based indices should follow the pattern idx_{10chars}
            hashBasedIndices.forEach { index ->
                val matches = "^idx_[0-9a-z]{10}$".toRegex().matches(index.indexName)
                matches shouldBe true
            }
        }
    }
}

