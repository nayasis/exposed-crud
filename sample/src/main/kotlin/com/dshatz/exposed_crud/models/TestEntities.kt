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
    var id: Int = -1,
    var name: String
)

/**
 * Test entity using LongIdTable
 */
@Entity
data class LongIdEntity(
    @Id(autoGenerate = true)
    var id: Long = -1,
    var name: String
)

/**
 * Test entity using UIntIdTable
 */
@Entity
data class UIntIdEntity(
    @Id(autoGenerate = true)
    var id: UInt = 0u,
    var name: String
)

/**
 * Test entity using ULongIdTable
 */
@Entity
data class ULongIdEntity(
    @Id(autoGenerate = true)
    var id: ULong = 0u,
    var name: String
)

/**
 * Test entity using UUIDTable
 */
@Entity
data class UUIDEntity(
    @Id(autoGenerate = true)
    var id: UUID = UUID.randomUUID(),
    var name: String
)


