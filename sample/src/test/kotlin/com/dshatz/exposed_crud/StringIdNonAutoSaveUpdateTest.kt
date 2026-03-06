package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.helper.TestHelper
import com.dshatz.exposed_crud.models.StringIdTimestampEntity
import com.dshatz.exposed_crud.models.StringIdTimestampEntityTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class StringIdNonAutoSaveUpdateTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = TestHelper.prepareDatabase(
            listOf(StringIdTimestampEntityTable),
        )
    }

    @Test
    fun `save should create when String ID does not exist for non-autogenerate entity`() {
        transaction(db) {
            val saved = StringIdTimestampEntityTable.repo.save(
                StringIdTimestampEntity(
                    code = "string-id-001",
                    name = "Created by save"
                )
            )

            saved.code shouldBe "string-id-001"
            saved.name shouldBe "Created by save"

            val found = StringIdTimestampEntityTable.repo.findById("string-id-001")
            found shouldNotBe null
            found?.name shouldBe "Created by save"
        }
    }

    @Test
    fun `save should update when String ID already exists for non-autogenerate entity`() {
        transaction(db) {
            StringIdTimestampEntityTable.repo.create(
                StringIdTimestampEntity(
                    code = "string-id-002",
                    name = "Original"
                )
            )

            val saved = StringIdTimestampEntityTable.repo.save(
                StringIdTimestampEntity(
                    code = "string-id-002",
                    name = "Updated by save"
                )
            )

            saved.code shouldBe "string-id-002"
            saved.name shouldBe "Updated by save"

            val all = StringIdTimestampEntityTable.repo.selectAll()
            all.size shouldBe 1
            all.first().name shouldBe "Updated by save"
        }
    }

    @Test
    fun `update should modify existing row for String ID non-autogenerate entity`() {
        transaction(db) {
            val created = StringIdTimestampEntityTable.repo.create(
                StringIdTimestampEntity(
                    code = "string-id-003",
                    name = "Before update"
                )
            )

            val updated = StringIdTimestampEntityTable.repo.update(
                created.copy(name = "After update")
            )

            updated.code shouldBe "string-id-003"
            updated.name shouldBe "After update"

            val found = StringIdTimestampEntityTable.repo.findById("string-id-003")
            found shouldNotBe null
            found?.name shouldBe "After update"
        }
    }
}
