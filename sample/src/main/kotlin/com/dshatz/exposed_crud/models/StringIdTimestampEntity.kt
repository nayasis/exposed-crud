package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.CreationTimestamp
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.UpdateTimestamp

/**
 * Entity for testing CreationTimestamp and UpdateTimestamp annotations with non-auto-incrementing ID (String)
 */
@Entity("string_id_timestamp_entities")
data class StringIdTimestampEntity(
    @Id
    val code: String,
    var name: String = "",
    @CreationTimestamp
    var createdAt: java.time.LocalDateTime? = null,
    @UpdateTimestamp
    var updatedAt: java.time.LocalDateTime? = null,
    @CreationTimestamp
    @UpdateTimestamp
    var tickedAt: kotlinx.datetime.LocalDateTime? = null,
)

