package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.CreationTimestamp
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.IdGenerator
import java.io.Serializable

class GeneratedIdTimestampEntityIdGenerator : IdGenerator<GeneratedIdTimestampEntity> {
    companion object {
        var seq = 0
    }

    override fun generate(entity: GeneratedIdTimestampEntity): Serializable {
        return "gid_${seq++}"
    }
}

@Entity("generated_id_timestamp_entities")
data class GeneratedIdTimestampEntity(
    @Id(autoGenerate = true, generator = GeneratedIdTimestampEntityIdGenerator::class)
    @Column
    var id: String = "",
    @Column
    var name: String = "",
    @Column
    @CreationTimestamp
    var createdAt: java.time.LocalDateTime? = null,
)
