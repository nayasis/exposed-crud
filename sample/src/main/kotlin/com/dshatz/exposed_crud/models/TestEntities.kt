package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import java.util.UUID

/**
 * Test entity using IntIdTable
 */
@Entity
data class IntIdEntity(
    @Id(autoGenerate = true)
    val id: Int = -1,
    val name: String
)

/**
 * Test entity using LongIdTable
 */
@Entity
data class LongIdEntity(
    @Id(autoGenerate = true)
    val id: Long = -1,
    val name: String
)

/**
 * Test entity using UIntIdTable
 */
@Entity
data class UIntIdEntity(
    @Id(autoGenerate = true)
    val id: UInt = 0u,
    val name: String
)

/**
 * Test entity using ULongIdTable
 */
@Entity
data class ULongIdEntity(
    @Id(autoGenerate = true)
    val id: ULong = 0u,
    val name: String
)

/**
 * Test entity using UUIDTable
 */
@Entity
data class UUIDEntity(
    @Id(autoGenerate = true)
    val id: UUID = UUID.randomUUID(),
    val name: String
)


