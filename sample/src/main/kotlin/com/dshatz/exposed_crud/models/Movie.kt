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
    @Id(autoGenerate = true)
    @Column
    var id: Long = -1,

    @Column
    val title: String,

    /*@DefaultText("01-01-1970")*/
    @Column
    val createdAt: Instant,

    @Column
    val originalTitle: String? = null,

    @ForeignKey(Director::class)
    @Column
    val directorId: Long,

    @ForeignKey(Category::class)
    @SerialName("category_id")
    @Column
    val categoryId: Long,

    @References(Director::class, "directorId")
    @Transient
    val director: Director? = null,

    @References(Category::class, "categoryId")
    @Transient
    val category: Category? = null
)