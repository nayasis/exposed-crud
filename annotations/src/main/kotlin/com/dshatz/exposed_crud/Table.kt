package com.dshatz.exposed_crud

@Target(AnnotationTarget.CLASS)
/**
 * Override the table name.
 * @param name table name to use in the database.
 * @param indexes Array of Index annotations to define indexes on the table.
 */
annotation class Table(
    val name: String = "",
    val indexes: Array<Index> = []
)
