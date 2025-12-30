package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.BackReference
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.JsonFormat
import com.dshatz.exposed_crud.MediumText
import com.dshatz.exposed_crud.Unique
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity
@Serializable
data class Director(
    @Id(true)
    var id: Long = -1,

    @Unique
    @MediumText
    var name: String,

    @BackReference(Movie::class)
    var movies: List<Movie>? = null,

    @com.dshatz.exposed_crud.Json("director")
    var oldDirector: Director? = null
)

@JsonFormat("director")
fun oldDirectorJsonFormat(): Json {
    return Json {
        prettyPrint = true
    }
}