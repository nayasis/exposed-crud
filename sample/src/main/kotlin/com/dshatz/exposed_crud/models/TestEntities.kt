package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import java.util.UUID

/**
 * Test entity using IntIdTable with mutable ID (var)
 */
@Entity
data class IntIdEntity(
    @Id(autoGenerate = true)
    var pkId: Int = -1,
    val name: String
)

/**
 * Test entity using LongIdTable with immutable ID (val)
 */
@Entity
data class LongIdEntity(
    @Id(autoGenerate = true)
    val pkId: Long = -1,
    val name: String
)

/**
 * Test entity using UIntIdTable with mutable ID (var)
 */
@Entity
data class UIntIdEntity(
    @Id(autoGenerate = true)
    var pkId: UInt = 0u,
    val name: String
)

/**
 * Test entity using ULongIdTable with immutable ID (val)
 */
@Entity
data class ULongIdEntity(
    @Id(autoGenerate = true)
    val pkId: ULong = 0u,
    val name: String
)