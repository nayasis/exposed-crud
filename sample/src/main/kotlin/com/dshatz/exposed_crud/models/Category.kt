package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.BackReference
import com.dshatz.exposed_crud.Default
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id

@Entity
data class Category(
    @Id(autoGenerate = true)
    var id: Long = -1,
    @Default("false")
    var adult: Boolean = false,

    @BackReference(CategoryTranslations::class)
    var translations: List<CategoryTranslations>? = null,

    @BackReference(Movie::class)
    var movies: List<Movie>? = null
)
