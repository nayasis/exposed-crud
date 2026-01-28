package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.BackReference
import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Json
import com.dshatz.exposed_crud.MediumText
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class Director(
    @Id(true)
    @Column
    var id: Long = -1,

    @Column(unique = true)
    @MediumText
    var name: String,

    @BackReference(Movie::class)
    var movies: List<Movie>? = null,

    @Column
    @Json
    var oldDirector: Director? = null
)