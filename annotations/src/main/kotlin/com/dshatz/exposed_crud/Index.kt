package com.dshatz.exposed_crud

/**
 * Defines an index on a table.
 * @param name The name of the index.
 * @param columnList Comma-separated string of column names to include in the index.
 * @param unique Whether the index should be unique (default: false).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Index(
    val name: String,
    val columnList: String,
    val unique: Boolean = false
)

