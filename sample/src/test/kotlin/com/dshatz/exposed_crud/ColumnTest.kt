package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.models.ColumnTestEntity
import com.dshatz.exposed_crud.models.ColumnTestEntityTable
import com.dshatz.exposed_crud.models.repo
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test

class ColumnTest {

    private lateinit var db: Database

    @BeforeTest
    fun init() {
        db = Database.connect("jdbc:sqlite:memory:column_test_${java.util.UUID.randomUUID()}?foreign_keys=on", "org.sqlite.JDBC")
        transaction(db) {
            SchemaUtils.drop(ColumnTestEntityTable)
            SchemaUtils.create(ColumnTestEntityTable)
        }
    }

    @Test
    fun `test Column annotation with default value and blank handling`() {
        transaction(db) {
            // 컬럼명 확인
            val columnNames = ColumnTestEntityTable.columns.map { it.name }.toSet()
            
            // @Column() - 기본값 사용 (blank) -> 프로퍼티 이름 기반으로 컬럼명 생성 (defaultColumn -> defaultColumn)
            columnNames shouldBe setOf("id", "defaultColumn", "blankColumn", "custom_name", "normalColumn")
            
            // 엔티티 생성 및 저장 테스트
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
            
            // 조회 테스트
            val found = ColumnTestEntityTable.repo.findById(entity.id)
            found shouldBe entity
        }
    }

    @Test
    fun `test Column annotation blank values are ignored`() {
        transaction(db) {
            // @Column()와 @Column("") 모두 프로퍼티 이름을 기반으로 컬럼명이 생성되는지 확인
            val columnNames = ColumnTestEntityTable.columns.map { it.name }.toSet()
            
            // @Column() - 기본값 사용 (blank) -> 프로퍼티 이름 기반으로 컬럼명 생성
            columnNames.contains("defaultColumn") shouldBe true
            
            // @Column("") - 명시적으로 빈 문자열 -> 프로퍼티 이름 기반으로 컬럼명 생성
            columnNames.contains("blankColumn") shouldBe true
            
            // @Column("custom_name") - 정상적인 이름 지정 -> 지정한 이름 사용
            columnNames.contains("custom_name") shouldBe true
            
            // 어노테이션 없음 -> 프로퍼티 이름 기반으로 컬럼명 생성
            columnNames.contains("normalColumn") shouldBe true
        }
    }
}

