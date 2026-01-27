package com.dshatz.exposed_crud


/**
 * Override column mapping details.
 *
 * @param name        column name to be used in the database. When blank, the property name is used.
 * @param unique      whether a unique index should be created for this single column.
 * @param definition  raw SQL fragment for the column type (DDL). When set, this overrides the default SQL type.
 * @param length      logical length for character columns (e.g. VARCHAR length). Defaults to 255.
 * @param precision   precision for decimal (exact numeric) columns (e.g. BigDecimal). Ignored when <= 0.
 * @param scale       scale for decimal (exact numeric) columns. Ignored when <= 0.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Column(
    val name: String = "",
    val unique: Boolean = false,
    val definition: String = "",
    val length: Int = 255,
    val precision: Int = 0,
    val scale: Int = 0,
)
