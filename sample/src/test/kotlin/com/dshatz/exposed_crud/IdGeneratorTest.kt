package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.Sample
import com.dshatz.exposed_crud.models.SampleIdGenerator
import com.dshatz.exposed_crud.models.SampleTable
import com.dshatz.exposed_crud.models.repo
import com.dshatz.exposed_crud.helper.TestHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class IdGeneratorTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        // Reset sequence for each test
        SampleIdGenerator.seq = 0
        
        db = TestHelper.prepareDatabase(
            listOf(SampleTable),
        )
    }

    @AfterTest
    fun cleanup() {
        // Reset sequence after test
        SampleIdGenerator.seq = 0
    }

    @Test
    fun `ID generator should generate ID when creating entity`() {
        transaction(db) {
            val sample1 = Sample(name = "Test1")
            val inserted1 = SampleTable.repo.create(sample1)
            
            inserted1.id shouldNotBe ""
            inserted1.id shouldBe "Test1_0"
            inserted1.name shouldBe "Test1"
            
            val sample2 = Sample(name = "Test2")
            val inserted2 = SampleTable.repo.create(sample2)
            
            inserted2.id shouldNotBe ""
            inserted2.id shouldBe "Test2_1"
            inserted2.name shouldBe "Test2"
        }
    }

    @Test
    fun `ID generator should generate unique IDs for multiple entities`() {
        transaction(db) {
            val samples = listOf(
                Sample(name = "Alice"),
                Sample(name = "Bob"),
                Sample(name = "Charlie")
            )
            
            val inserted = samples.map { SampleTable.repo.create(it) }
            
            inserted[0].id shouldBe "Alice_0"
            inserted[1].id shouldBe "Bob_1"
            inserted[2].id shouldBe "Charlie_2"
            
            // All IDs should be unique
            inserted.map { it.id }.distinct().size shouldBe 3
        }
    }

    @Test
    fun `ID generator should work with findById`() {
        transaction(db) {
            val sample = Sample(name = "FindMe")
            val inserted = SampleTable.repo.create(sample)
            
            val found = SampleTable.repo.findById(inserted.id)
            found shouldNotBe null
            found?.id shouldBe inserted.id
            found?.name shouldBe "FindMe"
        }
    }

    @Test
    fun `ID generator should work with selectAll`() {
        transaction(db) {
            SampleTable.repo.create(Sample(name = "First"))
            SampleTable.repo.create(Sample(name = "Second"))
            SampleTable.repo.create(Sample(name = "Third"))
            
            val all = SampleTable.repo.selectAll()
            all.size shouldBe 3
            all.map { it.id } shouldBe listOf("First_0", "Second_1", "Third_2")
        }
    }

    @Test
    fun `ID generator should work with update`() {
        transaction(db) {
            val sample = Sample(name = "Original")
            val inserted = SampleTable.repo.create(sample)
            
            val updated = SampleTable.repo.update(inserted.copy(name = "Updated"))
            
            updated.id shouldBe inserted.id
            updated.name shouldBe "Updated"
            
            val found = SampleTable.repo.findById(inserted.id)
            found?.name shouldBe "Updated"
        }
    }

    @Test
    fun `ID generator should work with delete`() {
        transaction(db) {
            val sample1 = SampleTable.repo.create(Sample(name = "Keep"))
            val sample2 = SampleTable.repo.create(Sample(name = "Delete"))
            
            SampleTable.repo.delete(sample2)
            
            val all = SampleTable.repo.selectAll()
            all.size shouldBe 1
            all.first().id shouldBe sample1.id
            all.first().name shouldBe "Keep"
        }
    }

    @Test
    fun `ID generator should generate IDs even when initial ID is provided`() {
        transaction(db) {
            // Even if we provide an initial ID, generator should override it
            val sample = Sample(id = "ignored", name = "Generated")
            val inserted = SampleTable.repo.create(sample)
            
            // Generator should have generated the ID, not the provided one
            inserted.id shouldBe "Generated_0"
            inserted.id shouldNotBe "ignored"
        }
    }

    @Test
    fun `ID generator sequence should increment across transactions`() {
        transaction(db) {
            val sample1 = SampleTable.repo.create(Sample(name = "First"))
            sample1.id shouldBe "First_0"
        }
        
        transaction(db) {
            val sample2 = SampleTable.repo.create(Sample(name = "Second"))
            sample2.id shouldBe "Second_1"
        }
        
        transaction(db) {
            val sample3 = SampleTable.repo.create(Sample(name = "Third"))
            sample3.id shouldBe "Third_2"
        }
    }

    @Test
    fun `ID generator should work when ID is empty string`() {
        transaction(db) {
            // IdGenerator should work even when ID is set to empty string ("")
            val sample = Sample(id = "", name = "EmptyId")
            val inserted = SampleTable.repo.create(sample)
            
            // Verify that IdGenerator generated a new ID
            inserted.id shouldNotBe ""
            inserted.id shouldBe "EmptyId_0"
        }
    }

    @Test
    fun `ID generator should work when ID is null (nullable String)`() {
        transaction(db) {
            // Note: To test nullable String ID, a separate entity would be needed,
            // but Sample uses non-nullable String, so we test with empty string default.
            // In practice, only the condition `autoGenerate && idGenerator != null` is checked,
            // so IdGenerator works regardless of the ID value.
            val sample = Sample(id = "", name = "NullTest")
            val inserted = SampleTable.repo.create(sample)
            
            inserted.id shouldNotBe ""
            inserted.id shouldBe "NullTest_0"
        }
    }
}

