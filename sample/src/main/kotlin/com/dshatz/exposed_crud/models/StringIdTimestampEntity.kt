package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
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
    @Column
    val code: String,
    @Column
    var name: String = "",
    @Column
    @CreationTimestamp
    var createdAt: java.time.LocalDateTime? = null,
    @Column
    @UpdateTimestamp
    var updatedAt: java.time.LocalDateTime? = null,
    @Column
    @CreationTimestamp
    @UpdateTimestamp
    var tickedAt: kotlinx.datetime.LocalDateTime? = null,
)

