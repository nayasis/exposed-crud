package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.*
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity
@Serializable
data class Movie(
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
    @Transient
    val director: Director? = null,

    @References(Category::class, "categoryId")
    @Transient
    val category: Category? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Movie

        if (id != other.id) return false
        if (title != other.title) return false
        // Compare createdAt at millisecond precision (DB stores only milliseconds)
        if (createdAt.toEpochMilliseconds() != other.createdAt.toEpochMilliseconds()) return false
        if (originalTitle != other.originalTitle) return false
        if (directorId != other.directorId) return false
        if (categoryId != other.categoryId) return false
        if (director != other.director) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}