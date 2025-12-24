package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Entity
@Serializable
data class Movie @OptIn(ExperimentalTime::class) constructor(
    @Id(autoGenerate = true) val id: Long = -1,

    val title: String,

    /*@DefaultText("01-01-1970")*/
    val createdAt: Instant,

    val originalTitle: String? = null,

    @ForeignKey(Director::class)
    val directorId: Long,

    @ForeignKey(Category::class)
    @SerialName("category_id")
    val categoryId: Long,

    @References(Director::class, "directorId")
    @kotlinx.serialization.Transient
    val director: Director? = null,

    @References(Category::class, "categoryId")
    @Transient
    val category: Category? = null
)