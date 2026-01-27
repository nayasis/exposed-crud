package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.IdGenerator
import java.io.Serializable

/**
 * Sample ID generator that generates IDs based on entity name and sequence
 */
class SampleIdGenerator : IdGenerator<Sample> {
    companion object {
        var seq = 0
    }

    override fun generate(entity: Sample): Serializable {
        return entity.name + "_${seq++}"
    }
}

/**
 * Sample entity using custom ID generator
 */
@Entity
data class Sample(
    @Id(autoGenerate = true, generator = SampleIdGenerator::class)
    @Column
    var id: String = "",
    @Column
    val name: String
)

