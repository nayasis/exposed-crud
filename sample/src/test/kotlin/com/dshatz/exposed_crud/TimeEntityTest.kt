package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.JavaTimeEntity
import com.dshatz.exposed_crud.models.JavaTimeEntityTable
import com.dshatz.exposed_crud.models.KotlinTimeEntity
import com.dshatz.exposed_crud.models.KotlinTimeEntityTable
import com.dshatz.exposed_crud.models.repo
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import kotlinx.datetime.LocalTime as KotlinLocalTime
import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlinx.datetime.toInstant
import java.time.Duration
import java.time.LocalDate as JavaLocalDate
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.LocalTime as JavaLocalTime
import kotlin.math.abs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

class TimeEntityTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect(
            "jdbc:sqlite:memory:time_db_${java.util.UUID.randomUUID()}?foreign_keys=on",
            "org.sqlite.JDBC"
        )
        transaction(db) {
            addLogger(StdOutSqlLogger)
            listOf(KotlinTimeEntityTable, JavaTimeEntityTable).forEach {
                SchemaUtils.drop(it)
                SchemaUtils.create(it)
            }
        }
    }

    @Test
    fun `kotlin time entity uses default timestamps dates and times`() {
        transaction(db) {
            val inserted = KotlinTimeEntityTable.repo.createReturning(
                KotlinTimeEntity(name = "kotlin-default")
            )

            inserted.createdAt shouldNotBe null
            inserted.updatedAt shouldBe null
            inserted.birthday shouldBe KotlinLocalDate(1990, 1, 1)
            inserted.wakeup shouldBe KotlinLocalTime(7, 0, 0)

            val zone = KotlinTimeZone.currentSystemDefault()
            val nowInstant = Clock.System.now()
            val createdInstant = inserted.createdAt.toInstant(zone)
            val deltaSeconds = abs(createdInstant.epochSeconds - nowInstant.epochSeconds)
            assert(deltaSeconds < 60) { "createdAt default should be close to now" }
        }
    }

    @Test
    fun `kotlin time entity allows overriding values`() {
        transaction(db) {
            val customCreated = KotlinLocalDateTime(2045, 3, 15, 10, 45, 0)
            val customUpdated = KotlinLocalDateTime(2045, 3, 16, 8, 0, 0)
            val customBirthday = KotlinLocalDate(1985, 8, 12)
            val customWakeup = KotlinLocalTime(5, 30, 0)

            val inserted = KotlinTimeEntityTable.repo.createReturning(
                KotlinTimeEntity(
                    name = "kotlin-custom",
                    createdAt = customCreated,
                    updatedAt = customUpdated,
                    birthday = customBirthday,
                    wakeup = customWakeup,
                )
            )

            inserted.createdAt shouldBe customCreated
            inserted.updatedAt shouldBe customUpdated
            inserted.birthday shouldBe customBirthday
            inserted.anniversary shouldBe null
            inserted.wakeup shouldBe customWakeup
        }
    }

    @Test
    fun `java time entity uses default timestamps dates and times`() {
        transaction(db) {
            val inserted = JavaTimeEntityTable.repo.createReturning(
                JavaTimeEntity(name = "java-default")
            )

            inserted.createdAt shouldNotBe null
            inserted.updatedAt shouldBe null
            inserted.birthday shouldBe JavaLocalDate.of(2000, 1, 1)
            inserted.anniversary shouldBe null
            inserted.wakeup shouldBe JavaLocalTime.of(7, 0)

            val now = JavaLocalDateTime.now()
            val deltaSeconds = Duration.between(inserted.createdAt, now).abs().seconds
            assert(deltaSeconds < 60) { "java.time createdAt default should be close to now" }
        }
    }

    @Test
    fun `java time entity allows overriding values`() {
        transaction(db) {
            val customCreated = JavaLocalDateTime.of(2050, 9, 1, 6, 15, 0)
            val customUpdated = JavaLocalDateTime.of(2050, 9, 2, 7, 0, 0)
            val customBirthday = JavaLocalDate.of(1975, 4, 22)
            val customAnniversary = JavaLocalDate.of(2010, 11, 5)
            val customWakeup = JavaLocalTime.of(4, 45)

            val inserted = JavaTimeEntityTable.repo.createReturning(
                JavaTimeEntity(
                    name = "java-custom",
                    createdAt = customCreated,
                    updatedAt = customUpdated,
                    birthday = customBirthday,
                    anniversary = customAnniversary,
                    wakeup = customWakeup,
                )
            )

            inserted.createdAt shouldBe customCreated
            inserted.updatedAt shouldBe customUpdated
            inserted.birthday shouldBe customBirthday
            inserted.anniversary shouldBe customAnniversary
            inserted.wakeup shouldBe customWakeup
        }
    }
}

