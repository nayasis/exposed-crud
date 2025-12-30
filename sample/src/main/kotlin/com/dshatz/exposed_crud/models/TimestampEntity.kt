package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.CreationTimestamp
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.UpdateTimestamp

/**
 * Entity for testing CreationTimestamp and UpdateTimestamp annotations
 */
@Entity("timestamp_entities")
data class TimestampEntity(
    @Id(autoGenerate = true)
    var id: Long = -1,
    var name: String = "",
    @CreationTimestamp
    var createdAt: java.time.LocalDateTime? = null,
    @UpdateTimestamp
    var updatedAt: java.time.LocalDateTime? = null,
    @CreationTimestamp
    @UpdateTimestamp
    var tickedAt: kotlinx.datetime.LocalDateTime? = null,
)
